/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.model.internal.inspect;

import org.gradle.api.Named;
import org.gradle.api.Nullable;
import org.gradle.model.internal.core.*;
import org.gradle.model.internal.core.rule.describe.ModelRuleDescriptor;
import org.gradle.model.internal.manage.instance.ManagedProxyFactory;
import org.gradle.model.internal.manage.projection.ManagedModelProjection;
import org.gradle.model.internal.manage.schema.*;
import org.gradle.model.internal.type.ModelType;

import java.util.Collections;
import java.util.List;

public class ManagedModelInitializer<T> implements NodeInitializer {

    protected final ModelManagedImplStructSchema<T> modelSchema;
    protected final ModelSchemaStore schemaStore;

    public ManagedModelInitializer(ModelManagedImplStructSchema<T> modelSchema, ModelSchemaStore schemaStore) {
        this.modelSchema = modelSchema;
        this.schemaStore = schemaStore;
    }

    @Override
    public List<? extends ModelReference<?>> getInputs() {
        return Collections.singletonList(ModelReference.of(NodeInitializerRegistry.class));
    }

    @Override
    public void execute(MutableModelNode modelNode, List<ModelView<?>> inputs) {
        NodeInitializerRegistry nodeInitializerRegistry = ModelViews.assertType(inputs.get(0), NodeInitializerRegistry.class).getInstance();
        for (ModelProperty<?> property : modelSchema.getProperties()) {
            addPropertyLink(modelNode, property, nodeInitializerRegistry);
        }
        if (isANamedType()) {
            // Only initialize "name" child node if the schema has such a managed property.
            // This is not the case for a managed subtype of an unmanaged type that implements Named.
            ModelProperty<?> nameProperty = modelSchema.getProperty("name");
            if (nameProperty != null && nameProperty.getStateManagementType().equals(ModelProperty.StateManagementType.MANAGED)) {
                MutableModelNode nameLink = modelNode.getLink("name");
                if (nameLink == null) {
                    throw new IllegalStateException("expected name node for " + modelNode.getPath());
                }
                nameLink.setPrivateData(ModelType.of(String.class), modelNode.getPath().getName());
            }
        }
    }

    @Override
    public List<? extends ModelProjection> getProjections() {
        return Collections.singletonList(new ManagedModelProjection<T>(modelSchema, schemaStore, ManagedProxyFactory.INSTANCE));
    }

    @Nullable
    @Override
    public ModelAction getProjector(ModelPath path, ModelRuleDescriptor descriptor) {
        return null;
    }

    private <P> void addPropertyLink(MutableModelNode modelNode, ModelProperty<P> property, NodeInitializerRegistry nodeInitializerRegistry) {
        // No need to create nodes for unmanaged properties
        if (!property.getStateManagementType().equals(ModelProperty.StateManagementType.MANAGED)) {
            return;
        }
        ModelType<P> propertyType = property.getType();
        ModelSchema<P> propertySchema = schemaStore.getSchema(propertyType);
        validateProperty(propertySchema, property, nodeInitializerRegistry);

        ModelRuleDescriptor descriptor = modelNode.getDescriptor();
        ModelPath childPath = modelNode.getPath().child(property.getName());
        if (propertySchema instanceof ManagedImplModelSchema) {
            if (!property.isWritable()) {
                ManagedImplModelSchema<P> managedPropertySchema = (ManagedImplModelSchema<P>) propertySchema;
                ModelCreator creator = ModelCreators.of(childPath, nodeInitializerRegistry.getNodeInitializer(managedPropertySchema))
                    .descriptor(descriptor)
                    .build();
                modelNode.addLink(creator);
            } else {
                if (propertySchema instanceof ScalarCollectionSchema) {
                    ManagedImplModelSchema<P> managedPropertySchema = (ManagedImplModelSchema<P>) propertySchema;
                    ModelCreator creator = ModelCreators.of(childPath, nodeInitializerRegistry.getNodeInitializer(managedPropertySchema))
                        .descriptor(descriptor)
                        .build();
                    modelNode.addLink(creator);
                } else {
                    ModelManagedImplStructSchema<P> structSchema = (ModelManagedImplStructSchema<P>) propertySchema;
                    ModelProjection projection = new ManagedModelProjection<P>(structSchema, schemaStore, ManagedProxyFactory.INSTANCE);
                    ModelCreator creator = ModelCreators.of(childPath)
                        .withProjection(projection)
                        .descriptor(descriptor).build();
                    modelNode.addReference(creator);
                }
            }
        } else {
            ModelProjection projection = new UnmanagedModelProjection<P>(propertyType, true, true);
            ModelCreators.Builder creatorBuilder;
            if (shouldHaveANodeInitializer(property, propertySchema)) {
                creatorBuilder = ModelCreators.of(childPath, nodeInitializerRegistry.getNodeInitializer(propertyType));
            } else {
                creatorBuilder = ModelCreators.of(childPath);
            }
            creatorBuilder
                .withProjection(projection)
                .descriptor(descriptor);
            modelNode.addLink(creatorBuilder.build());
        }
    }

    private <P> void validateProperty(ModelSchema<P> propertySchema, ModelProperty<P> property, NodeInitializerRegistry nodeInitializerRegistry) {
        if (propertySchema instanceof ManagedImplModelSchema) {
            if (propertySchema instanceof ModelCollectionSchema) {
                ModelCollectionSchema<P, ?> propertyCollectionsSchema = (ModelCollectionSchema<P, ?>) propertySchema;
                ModelType<?> elementType = propertyCollectionsSchema.getElementType();
                if (!(propertySchema instanceof ScalarCollectionSchema)) {
                    if (!property.isWritable()) {
                        nodeInitializerRegistry.ensureHasInitializer(elementType);
                    }
                }
            }
            if (!property.isWritable()) {
                if (property.isDeclaredAsHavingUnmanagedType()) {
                    throw new UnmanagedPropertyMissingSetterException(property);
                }
            }
        } else if (!shouldHaveANodeInitializer(property, propertySchema) && !property.isWritable() && !isNamePropertyOfANamedType(property)) {
            throw new ReadonlyImmutableManagedPropertyException(modelSchema.getType(), property.getName(), property.getType());
        }
    }

    private <P> boolean isNamePropertyOfANamedType(ModelProperty<P> property) {
        return isANamedType() && "name".equals(property.getName());
    }

    public boolean isANamedType() {
        return Named.class.isAssignableFrom(modelSchema.getType().getRawClass());
    }

    private <P> boolean shouldHaveANodeInitializer(ModelProperty<P> property, ModelSchema<P> propertySchema) {
        return !isAModelValueSchema(propertySchema) && !property.isDeclaredAsHavingUnmanagedType();
    }

    private <P> boolean isAModelValueSchema(ModelSchema<P> propertySchema) {
        return propertySchema instanceof ModelValueSchema;
    }
}
