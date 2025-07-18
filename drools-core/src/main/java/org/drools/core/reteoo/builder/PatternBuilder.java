/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.core.reteoo.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.drools.base.base.ClassObjectType;
import org.drools.base.base.DroolsQuery;
import org.drools.base.base.ObjectType;
import org.drools.base.rule.Accumulate;
import org.drools.base.rule.Behavior;
import org.drools.base.rule.Declaration;
import org.drools.base.rule.EntryPointId;
import org.drools.base.rule.GroupElement;
import org.drools.base.rule.IntervalProviderConstraint;
import org.drools.base.rule.InvalidPatternException;
import org.drools.base.rule.Pattern;
import org.drools.base.rule.PatternSource;
import org.drools.base.rule.RuleConditionElement;
import org.drools.base.rule.TypeDeclaration;
import org.drools.base.rule.WindowReference;
import org.drools.base.rule.constraint.AlphaNodeFieldConstraint;
import org.drools.base.rule.constraint.BetaConstraint;
import org.drools.base.rule.constraint.Constraint;
import org.drools.base.rule.constraint.XpathConstraint;
import org.drools.base.time.impl.Timer;
import org.drools.core.reteoo.CoreComponentFactory;
import org.drools.core.reteoo.EntryPointNode;
import org.drools.core.reteoo.ObjectTypeNode;
import org.drools.core.reteoo.WindowNode;
import org.drools.core.rule.BehaviorRuntime;
import org.drools.core.time.impl.CompositeMaxDurationTimer;
import org.drools.core.time.impl.DurationTimer;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.definition.type.Expires.Policy;

import static org.drools.base.rule.TypeDeclaration.NEVER_EXPIRES;
import static org.drools.core.reteoo.builder.GroupElementBuilder.AndBuilder.buildJoinNode;
import static org.drools.core.reteoo.builder.GroupElementBuilder.AndBuilder.buildTupleSource;
/**
 * A builder for patterns
 */
public class PatternBuilder
    implements
    ReteooComponentBuilder {

    /**
     * @inheritDoc
     */
    public void build(final BuildContext context,
                      final BuildUtils utils,
                      final RuleConditionElement rce) {

        final Pattern pattern = (Pattern) rce;

        if (!(pattern.getSource() instanceof Accumulate) ) {
            // if the pattern has an accumulate as source it won't be relevant for calculation of property reactivity masks
            context.setLastBuiltPattern( pattern );
        }

        context.pushRuleComponent( pattern );
        context.syncObjectTypesWithObjectCount(); // must unwind object types from subnetworks, if they exist
        this.attachPattern( context,
                            utils,
                            pattern );
        // mus be added after
        context.addPattern( pattern );
        context.popRuleComponent();
    }

    private void attachPattern(final BuildContext context,
                               final BuildUtils utils,
                               final Pattern pattern) throws InvalidPatternException {
        Constraints constraints = createConstraints(context, pattern);

        // Set pattern tuple index and objectIndex to the appropriate value
        pattern.setTupleIndex(context.getTupleSource() == null ? 0 : context.getTupleSource().getPathIndex() + 1);
        pattern.setObjectIndex((context.getTupleSource() != null) ? context.getTupleSource().getObjectCount() : 0);

        // Create BetaConstraints object
        context.setBetaconstraints( constraints.betaConstraints );

        if ( pattern.getSource() != null ) {
            context.setAlphaConstraints( constraints.alphaConstraints );

            PatternSource source = pattern.getSource();

            ReteooComponentBuilder builder = utils.getBuilderFor( source );
            
            if( builder == null ) {
                throw new RuntimeException( "Unknown pattern source type: "+source.getClass()+" for source "+source+" on pattern "+pattern );
            }

            builder.build( context, utils, source );
        } else {
            // default entry point
            PatternSource source = EntryPointId.DEFAULT;
            ReteooComponentBuilder builder = utils.getBuilderFor( source );
            builder.build( context, utils, source );
        }

        buildBehaviors(context, utils, pattern, constraints);

        if ( context.getObjectSource() != null ) {
            attachAlphaNodes( context, utils, constraints.alphaConstraints );
        }

        buildXpathConstraints(context, utils, constraints);
    }

    private void buildBehaviors(BuildContext context, BuildUtils utils, Pattern pattern, Constraints constraints) {
        if ( pattern.getSource() == null ||
             ( !( pattern.getSource() instanceof WindowReference) &&
               ( context.getCurrentEntryPoint() != EntryPointId.DEFAULT || !pattern.getBehaviors().isEmpty() ) ) ){
            attachObjectTypeNode( context, utils, pattern );
        }

        if( !pattern.getBehaviors().isEmpty() ) {
            final List<BehaviorRuntime> behaviors = pattern.getBehaviors().stream().map(BehaviorRuntime.class::cast).collect(Collectors.toList());

            // build the window node:
            WindowNode wn = CoreComponentFactory.get().getNodeFactoryService().buildWindowNode( context.getNextNodeId(),
                                                                                                   constraints.alphaConstraints,
                                                                                                   behaviors,
                                                                                                   context.getObjectSource(),
                                                                                                   context );
            context.setObjectSource( utils.attachNode( context, wn ) );

            // alpha constraints added to the window node already
            constraints.alphaConstraints.clear();
        }
    }

    private void buildXpathConstraints(BuildContext context, BuildUtils utils, Constraints constraints) {
        if (!constraints.xpathConstraints.isEmpty()) {
            buildTupleSource(context, utils, false);

            if (constraints.xpathConstraints.size() == 1 && constraints.xpathConstraints.get(0).getXpathStartDeclaration() != null) {
                context.setObjectSource( null );
            } else {
                buildJoinNode( context, utils );
            }

            ReteooComponentBuilder builder = utils.getBuilderFor(XpathConstraint.class);
            for (XpathConstraint xpathConstraint : constraints.xpathConstraints) {
                for (XpathConstraint.XpathChunk chunk : xpathConstraint.getChunks()) {
                    context.setAlphaConstraints(chunk.getAlphaConstraints());
                    context.setBetaconstraints(chunk.getBetaConstraints());
                    context.setXpathConstraints(chunk.getXpathConstraints());
                    builder.build(context, utils, chunk.asFrom());
                }
            }

            context.popRuleComponent();
        }
    }

    private Constraints createConstraints(BuildContext context, Pattern pattern) {
        Constraints constraints = new Constraints();
        // check if cross products for identity patterns should be disabled
        checkRemoveIdentities( context,
                               pattern,
                               constraints.betaConstraints );

        // checks if this pattern is nested inside a NOT CE
        final boolean isNegative = isNegative( context );

        for ( Constraint constraint : pattern.getConstraints() ) {
            switch (constraint.getType()) {
                case ALPHA:
                    linkAlphaConstraint( (AlphaNodeFieldConstraint) constraint, constraints.alphaConstraints );
                    break;
                case BETA:
                    linkBetaConstraint((BetaConstraint) constraint, constraints.betaConstraints);
                    if ( isNegative && context.getRuleBase().getRuleBaseConfiguration().getEventProcessingMode() == EventProcessingOption.STREAM && pattern.getObjectType().isEvent() && constraint.isTemporal() ) {
                        checkDelaying( context, constraint );
                    }
                    break;
                case XPATH:
                    constraints.xpathConstraints.add( (XpathConstraint) constraint );
                    break;
                default:
                    throw new RuntimeException( "Unknown constraint type: " + constraint.getType() + ". This is a bug. Please contact development team." );
            }
        }
        return constraints;
    }

    protected void linkBetaConstraint(BetaConstraint constraint, List<BetaConstraint> betaConstraints) {
        betaConstraints.add( constraint );
    }

    protected void linkAlphaConstraint( AlphaNodeFieldConstraint constraint, List<AlphaNodeFieldConstraint> alphaConstraints ) {
        alphaConstraints.add( constraint );
    }


    private void checkDelaying(final BuildContext context,
                               final Constraint constraint) {
        if ( constraint instanceof IntervalProviderConstraint ) {
            // variable constraints always require a single declaration
            Declaration target = constraint.getRequiredDeclarations()[0];
            if ( target.isPatternDeclaration() && target.getPattern().getObjectType().isEvent() ) {
                long uplimit = ((IntervalProviderConstraint) constraint).getInterval().getUpperBound();
                // only makes sense to add the new timer if the uplimit is not infinity (Long.MAX_VALUE)
                if( uplimit >= 0 && uplimit < Long.MAX_VALUE ) {
                    Timer timer = context.getRule().getTimer();
                    DurationTimer durationTimer = new DurationTimer( uplimit );
                    durationTimer.setEventFactHandle(target);

                    if ( timer instanceof CompositeMaxDurationTimer ) {
                        // already a composite so just add
                        ((CompositeMaxDurationTimer) timer).addDurationTimer( durationTimer );
                    } else {
                        if ( timer == null ) {
                            // no timer exists, so ok on it's own
                            timer = durationTimer;
                        } else {
                            // timer exists so we need to make a composite
                            CompositeMaxDurationTimer temp = new CompositeMaxDurationTimer();
                            if ( timer instanceof DurationTimer ) {
                                // previous timer was a duration, so add another DurationTimer
                                temp.addDurationTimer( (DurationTimer) timer );
                            } else {
                                // previous timer was not a duration, so set it as the delegate Timer.
                                temp.setTimer( context.getRule().getTimer() );
                            }
                            // now add the new durationTimer
                            temp.addDurationTimer( durationTimer );
                            timer = temp;
                        }
                        // with the composite made, reset it on the Rule
                        context.getRule().setTimer( timer );
                    }
                }
            }
        }
    }

    private boolean isNegative(final BuildContext context) {
        for ( RuleConditionElement rce : context.getBuildstack() ) {
            if ( rce instanceof GroupElement && ((GroupElement) rce).isNot() ) {
                return true;
            }
        }
        return false;
    }

    private static ExpirationSpec getExpirationForType( BuildContext context,
                                              ObjectType objectType ) {
        long offset = NEVER_EXPIRES;
        boolean hard = false;

        for (TypeDeclaration type : context.getRuleBase().getTypeDeclarations()) {
            if (type.getObjectType().isAssignableFrom( objectType )) {
                if ( hard ) {
                    if ( type.getExpirationPolicy() == Policy.TIME_HARD && type.getExpirationOffset() > offset ) {
                        offset = type.getExpirationOffset();
                    }
                } else {
                    if ( type.getExpirationPolicy() == Policy.TIME_HARD ) {
                        offset = type.getExpirationOffset();
                        hard = true;
                    } else if ( type.getExpirationOffset() > offset ) {
                        offset = type.getExpirationOffset();
                    }
                }
            }
        }

        // if none of the type declarations have an @expires annotation
        // we return -1 (no-expiration) value, otherwise we return the
        // set expiration value+1 to enable the fact to match events with
        // the same timestamp
        return new ExpirationSpec( offset == NEVER_EXPIRES ? NEVER_EXPIRES : offset+1, hard );
    }

    private static class ExpirationSpec {
        final long offset;
        final boolean hard;

        private ExpirationSpec( long offset, boolean hard ) {
            this.offset = offset;
            this.hard = hard;
        }
    }

    public void attachAlphaNodes(final BuildContext context,
                                 final BuildUtils utils,
                                 final List<AlphaNodeFieldConstraint> alphaConstraints) throws InvalidPatternException {

        // Drools Query ObjectTypeNode never has memory, but other ObjectTypeNode/AlphaNoesNodes may (if not in sequential), 
        //so need to preserve, so we can restore after this node is added. LeftMemory  and Terminal remain the same once set.

        buildAlphaNodeChain( context, utils, alphaConstraints );

        NodeFactory nfactory = CoreComponentFactory.get().getNodeFactoryService();
        
        if ( context.getCurrentEntryPoint() != EntryPointId.DEFAULT && context.isAttachPQN() ) {
            // the entry-point specific network nodes are attached, so, set context to default entry-point
            context.setCurrentEntryPoint( EntryPointId.DEFAULT );
        }
    }

    private void buildAlphaNodeChain( BuildContext context, BuildUtils utils, List<AlphaNodeFieldConstraint> alphaConstraints ) {
        for ( final AlphaNodeFieldConstraint constraint : alphaConstraints ) {
            context.pushRuleComponent( constraint );
            context.setObjectSource( utils.attachNode( context,
                    CoreComponentFactory.get().getNodeFactoryService().buildAlphaNode( context.getNextNodeId(),
                                                                                                                             constraint,
                                                                                                                             context.getObjectSource(),
                                                                                                                             context) ) );
            context.popRuleComponent();
        }
    }

    private void attachObjectTypeNode( final BuildContext context, final BuildUtils utils, final Pattern pattern ) {
        ObjectType objectType = pattern.getObjectType();
        
        if ( pattern.getObjectType() instanceof ClassObjectType ) {
            // Is this the query node, if so we don't want any memory
            if (DroolsQuery.class == ((ClassObjectType) pattern.getObjectType()).getClassType() ) {
                context.setTupleMemoryEnabled( false );
            }
        }

        ObjectTypeNode otn = CoreComponentFactory.get().getNodeFactoryService().buildObjectTypeNode( context.getNextNodeId(),
                                                 (EntryPointNode) context.getObjectSource(),
                                                 objectType,
                                                 context );
        if ( objectType.isEvent() && EventProcessingOption.STREAM.equals( context.getRuleBase().getRuleBaseConfiguration().getEventProcessingMode() ) ) {
            ExpirationSpec expirationSpec = getExpirationForType( context, objectType );

            if( expirationSpec.offset != NEVER_EXPIRES && expirationSpec.hard ) {
                // hard expiration is set, so use it
                otn.setExpirationOffset( expirationSpec.offset );
            } else {
                // otherwise calculate it based on behaviours and temporal constraints
                long offset = NEVER_EXPIRES;
                for ( Behavior behavior : pattern.getBehaviors() ) {
                    if ( behavior.getExpirationOffset() != NEVER_EXPIRES ) {
                        offset = Math.max( behavior.getExpirationOffset(), offset );
                    }
                }

                // if there's no implicit expiration uses the (eventually set) soft one
                if (offset == NEVER_EXPIRES && !expirationSpec.hard) {
                    offset = expirationSpec.offset;
                }

                long distance = context.getExpirationOffset( pattern );
                if ( distance == NEVER_EXPIRES ) {
                    // it means the rules have no temporal constraints, or
                    // the constraints require events to be hold forever. In this 
                    // case, we allow type declarations to override the implicit 
                    // expiration offset by defining an expiration policy with the
                    // @expires tag
                    otn.setExpirationOffset( offset );
                } else {
                    otn.setExpirationOffset( Math.max( distance, offset ) );
                }
            }
        }

        context.setObjectSource( utils.attachNode( context, otn ) );
    }

    /**
     * @param context
     * @param pattern
     * @param betaConstraints
     */
    private void checkRemoveIdentities(final BuildContext context,
                                       final Pattern pattern,
                                       final List<BetaConstraint> betaConstraints) {
        if ( context.getRuleBase().getRuleBaseConfiguration().isRemoveIdentities() && pattern.getObjectType().getClass() == ClassObjectType.class ) {
            // Check if this object type exists before
            // If it does we need stop instance equals cross product
            final ObjectType thisObjectType = pattern.getObjectType();
            for ( final Pattern previousPattern : context.getPatterns() ) {
                final ObjectType previousObjectType = previousPattern.getObjectType();
                if ( thisObjectType.isAssignableFrom( previousObjectType ) ) {
                    betaConstraints.add( new InstanceNotEqualsConstraint( previousPattern ) );
                }
            }
        }
    }

    /**
     * @inheritDoc
     */
    public boolean requiresLeftActivation(final BuildUtils utils,
                                          final RuleConditionElement rce) {
        PatternSource source = ((Pattern) rce).getSource();
        return (source != null && source.requiresLeftActivation() ) ||
               ! ((Pattern) rce).getBehaviors().isEmpty() ;
    }

    private static class Constraints {
        private final List<AlphaNodeFieldConstraint> alphaConstraints = new ArrayList<>();
        private final List<BetaConstraint>           betaConstraints  = new ArrayList<>();
        private final List<XpathConstraint>          xpathConstraints = new ArrayList<>();
    }
}
