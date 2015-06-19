/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.api.internal.artifacts.ivyservice;

import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.internal.artifacts.*;
import org.gradle.api.internal.artifacts.component.ComponentIdentifierFactory;
import org.gradle.api.internal.artifacts.configurations.DependencyMetaDataProvider;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.projectresult.DefaultResolvedLocalComponentsResultBuilder;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.projectresult.ResolvedLocalComponentsResult;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.DefaultResolutionResultBuilder;
import org.gradle.api.internal.artifacts.repositories.ResolutionAwareRepository;
import org.gradle.api.specs.Spec;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ShortcircuitEmptyConfigsArtifactDependencyResolver implements ArtifactDependencyResolver {
    private final ArtifactDependencyResolver dependencyResolver;
    private final ComponentIdentifierFactory componentIdentifierFactory;

    public ShortcircuitEmptyConfigsArtifactDependencyResolver(ArtifactDependencyResolver dependencyResolver, ComponentIdentifierFactory componentIdentifierFactory) {
        this.dependencyResolver = dependencyResolver;
        this.componentIdentifierFactory = componentIdentifierFactory;
    }

    public void resolve(ResolveContext resolveContext,
                        List<? extends ResolutionAwareRepository> repositories,
                        GlobalDependencyResolutionRules metadataHandler,
                        ResolverResults results) throws ResolveException {
        if (resolveContext instanceof Configuration && resolveContext.getAllDependencies().isEmpty()) {
            ModuleInternal module = ((DependencyMetaDataProvider) resolveContext).getModule();
            ModuleVersionIdentifier id = DefaultModuleVersionIdentifier.newId(module);
            ComponentIdentifier componentIdentifier = componentIdentifierFactory.createComponentIdentifier(module);
            ResolutionResult emptyResult = new DefaultResolutionResultBuilder().start(id, componentIdentifier).complete();
            ResolvedLocalComponentsResult emptyProjectResult = new DefaultResolvedLocalComponentsResultBuilder(false).complete();
            results.resolved(emptyResult, emptyProjectResult);
        } else {
            dependencyResolver.resolve(resolveContext, repositories, metadataHandler, results);
        }
    }

    @Override
    public void resolveArtifacts(ResolveContext resolveContext, List<? extends ResolutionAwareRepository> repositories, GlobalDependencyResolutionRules metadataHandler, ResolverResults results) throws ResolveException {
        dependencyResolver.resolveArtifacts(resolveContext, repositories, metadataHandler, results);
    }

    public static class EmptyResolvedConfiguration implements ResolvedConfiguration {

        public boolean hasError() {
            return false;
        }

        public LenientConfiguration getLenientConfiguration() {
            return new LenientConfiguration() {
                public Set<ResolvedDependency> getFirstLevelModuleDependencies(Spec<? super Dependency> dependencySpec) {
                    return Collections.emptySet();
                }

                public Set<UnresolvedDependency> getUnresolvedModuleDependencies() {
                    return Collections.emptySet();
                }

                public Set<File> getFiles(Spec<? super Dependency> dependencySpec) {
                    return Collections.emptySet();
                }

                public Set<ResolvedArtifact> getArtifacts(Spec<? super Dependency> dependencySpec) {
                    return Collections.emptySet();
                }
            };
        }

        public void rethrowFailure() throws ResolveException {
        }

        public Set<File> getFiles(Spec<? super Dependency> dependencySpec) {
            return Collections.emptySet();
        }

        public Set<ResolvedDependency> getFirstLevelModuleDependencies() {
            return Collections.emptySet();
        }

        public Set<ResolvedDependency> getFirstLevelModuleDependencies(Spec<? super Dependency> dependencySpec) throws ResolveException {
            return Collections.emptySet();
        }

        public Set<ResolvedArtifact> getResolvedArtifacts() {
            return Collections.emptySet();
        }
    }
}
