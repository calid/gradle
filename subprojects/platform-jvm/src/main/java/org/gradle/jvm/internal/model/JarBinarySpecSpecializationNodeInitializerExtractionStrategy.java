/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.jvm.internal.model;

import org.gradle.jvm.JarBinarySpec;
import org.gradle.model.internal.core.NodeInitializer;
import org.gradle.model.internal.manage.schema.ModelManagedImplStructSchema;
import org.gradle.model.internal.manage.schema.ModelSchemaStore;
import org.gradle.model.internal.manage.schema.extract.ManagedImplStructNodeInitializerExtractionSupport;

public class JarBinarySpecSpecializationNodeInitializerExtractionStrategy extends ManagedImplStructNodeInitializerExtractionSupport {
    private final ModelSchemaStore schemaStore;

    public JarBinarySpecSpecializationNodeInitializerExtractionStrategy(ModelSchemaStore schemaStore) {
        super(JarBinarySpec.class);
        this.schemaStore = schemaStore;
    }

    @Override
    protected <T> NodeInitializer extractNodeInitializer(ModelManagedImplStructSchema<T> schema) {
        return new JarBinarySpecSpecializationModelInitializer<T>(schema, schemaStore);
    }
}
