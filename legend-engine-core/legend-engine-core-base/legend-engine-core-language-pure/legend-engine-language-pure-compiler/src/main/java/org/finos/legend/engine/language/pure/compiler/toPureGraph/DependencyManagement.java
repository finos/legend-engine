// Copyright 2024 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.FixedSizeList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.PackageableElement;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class DependencyManagement
{
    private final MutableMap<Class<? extends PackageableElement>, Collection<Class<? extends PackageableElement>>> dependentToDependencies = Maps.mutable.empty();
    private final MutableMap<Class<? extends PackageableElement>, Collection<Class<? extends PackageableElement>>> dependencyToDependents = Maps.mutable.empty();
    private final MutableList<MutableSet<Class<? extends PackageableElement>>> topologicallyOrderedClasses = Lists.mutable.empty();
    private final MutableMap<Class<? extends PackageableElement>, Integer> rankBySize = Maps.mutable.empty();
    private final MutableMap<Class<? extends PackageableElement>, Class<? extends PackageableElement>> unionFind = Maps.mutable.empty();
    private final MutableSet<MutableSet<java.lang.Class<? extends PackageableElement>>> disjointDependencyGraphs = Sets.mutable.empty();

    public void processDependencyGraph(MutableMap<Class<? extends PackageableElement>, Collection<Class<? extends PackageableElement>>> dependencyGraph)
    {
        MutableSet<Class<? extends PackageableElement>> abstractClasses = Sets.mutable.empty();
        dependencyGraph.forEach((dependent, dependencies) ->
        {
            dependentToDependencies.put(dependent, dependencies);
            unionFind.putIfAbsent(dependent, dependent);
            rankBySize.putIfAbsent(dependent, 1);
            collectAbstractClasses(dependent, abstractClasses);
            dependencies.forEach(dependency ->
            {
                if (!dependencyGraph.containsKey(dependency))
                {
                    dependentToDependencies.putIfAbsent(dependency, Lists.mutable.empty());
                }
                unionFind.putIfAbsent(dependency, dependency);
                rankBySize.putIfAbsent(dependency, 1);
                collectAbstractClasses(dependency, abstractClasses);
            });
        });

        dependentToDependencies.forEachKey(clazz ->
            abstractClasses.forEach(abstractClass ->
            {
                if (abstractClass != clazz && abstractClass.isAssignableFrom(clazz))
                {
                    dependentToDependencies.get(abstractClass).add(clazz);
                }
            }));
    }

    private void collectAbstractClasses(Class<? extends PackageableElement> clazz, MutableSet<Class<? extends PackageableElement>> abstractClasses)
    {
        if (Modifier.isAbstract(clazz.getModifiers()))
        {
            abstractClasses.add(clazz);
        }
    }

    public void detectCircularDependency()
    {
        MutableMap<Class<? extends PackageableElement>, Integer> inDegrees = Maps.mutable.empty();
        dependentToDependencies.forEach((dependent, dependencies) ->
        {
            dependencyToDependents.putIfAbsent(dependent, Lists.mutable.empty());
            inDegrees.putIfAbsent(dependent, 0);
            dependencies.forEach(dependency ->
            {
                dependencyToDependents.putIfAbsent(dependency, Lists.mutable.empty());
                dependencyToDependents.get(dependency).add(dependent);
                inDegrees.put(dependent, inDegrees.get(dependent) + 1);
            });
        });

        Queue<Class<? extends PackageableElement>> queue = new LinkedList<>();
        inDegrees.forEach((dependency, inDegree) ->
        {
            if (inDegree == 0)
            {
                queue.offer(dependency);
            }
        });

        int sortedClassCount = 0;
        while (!queue.isEmpty())
        {
            int size = queue.size();
            MutableSet<Class<? extends PackageableElement>> independentClasses = Sets.mutable.empty();
            for (int i = 0; i < size; i++)
            {
                Class<? extends PackageableElement> dependency = queue.poll();
                independentClasses.add(dependency);
                sortedClassCount++;
                dependencyToDependents.get(dependency).forEach(dependent ->
                {
                    inDegrees.put(dependent, inDegrees.getOrDefault(dependent, 0) - 1);
                    if (inDegrees.get(dependent) == 0)
                    {
                        queue.offer(dependent);
                    }
                });
            }
            topologicallyOrderedClasses.add(independentClasses);
        }

        if (sortedClassCount != dependencyToDependents.size())
        {
            throw new EngineException("Detected a circular dependency in dependencyGraph", EngineErrorType.COMPILATION);
        }
    }

    public FastListMultimap<java.lang.Class<? extends PackageableElement>, PackageableElementsByDependencyLevel> topologicallySortElements(FastListMultimap<java.lang.Class<? extends PackageableElement>, PackageableElementsByDependencyLevel> classToElements, MutableMap<java.lang.Class<? extends PackageableElement>, MutableMap<String, MutableSet<String>>> elementPrerequisitesByClass)
    {
        FastListMultimap<java.lang.Class<? extends PackageableElement>, PackageableElementsByDependencyLevel> classToElementsSortedByDependencyLevel = new FastListMultimap<>();
        classToElements.forEachKeyMultiValues((clazz, elementsByDependencyLevel) ->
        {
            if (elementPrerequisitesByClass.containsKey(clazz))
            {
                MutableMap<String, MutableSet<String>> elementPrerequisites = elementPrerequisitesByClass.get(clazz);
                MutableMap<String, Integer> inDegrees = Maps.mutable.empty();
                MutableMap<String, List<String>> dependencyToDependents = Maps.mutable.empty();
                elementPrerequisites.forEachKeyValue((dependentElementFullPath, prerequisiteFullPaths) ->
                {
                    dependencyToDependents.putIfAbsent(dependentElementFullPath, Lists.mutable.empty());
                    inDegrees.put(dependentElementFullPath, inDegrees.getOrDefault(dependentElementFullPath, 0) + prerequisiteFullPaths.size());
                    prerequisiteFullPaths.forEach(prerequisiteFullPath ->
                    {
                        dependencyToDependents.putIfAbsent(prerequisiteFullPath, Lists.mutable.empty());
                        dependencyToDependents.get(prerequisiteFullPath).add(dependentElementFullPath);
                    });
                });
                MutableMap<String, PackageableElement> packageableElementsIndex = Maps.mutable.empty();
                elementsByDependencyLevel.forEach(elementsInCurrentDependencyLevel ->
                {
                    elementsInCurrentDependencyLevel.getIndependentElementAndPathPairs().forEach(elementAndPathPair ->
                    {
                        PackageableElement element = elementAndPathPair.getOne();
                        String elementFullPath = elementAndPathPair.getTwo();
                        packageableElementsIndex.put(elementFullPath, element);
                        inDegrees.putIfAbsent(elementFullPath, 0);
                    });
                });

                Queue<String> queue = new LinkedList<>();
                inDegrees.forEach((elementFullPath, inDegree) ->
                {
                    if (inDegree == 0)
                    {
                        queue.offer(elementFullPath);
                    }
                });

                int sortedElementCount = 0;
                MutableList<PackageableElementsByDependencyLevel> elementsSortedByDependencyLevel = Lists.mutable.empty();
                while (!queue.isEmpty())
                {
                    int size = queue.size();
                    MutableList<Pair<PackageableElement, String>> currentDependencyLevelElements = Lists.mutable.empty();
                    for (int i = 0; i < size; i++)
                    {
                        String elementFullPath = queue.poll();
                        currentDependencyLevelElements.add(Tuples.pair(packageableElementsIndex.get(elementFullPath), elementFullPath));
                        sortedElementCount++;
                        dependencyToDependents.getOrDefault(elementFullPath, Lists.fixedSize.empty()).forEach(dependent ->
                        {
                            inDegrees.put(dependent, inDegrees.getOrDefault(dependent, 0) - 1);
                            if (inDegrees.get(dependent) == 0)
                            {
                                queue.offer(dependent);
                            }
                        });
                    }
                    elementsSortedByDependencyLevel.add(new PackageableElementsByDependencyLevel(currentDependencyLevelElements));
                }

                if (sortedElementCount != packageableElementsIndex.size())
                {
                    throw new EngineException("Detected a circular dependency in element prerequisites graph in the following metamodel: " + clazz + ".\nCycle: " + getElementsInCircularDependency(elementPrerequisites), EngineErrorType.COMPILATION);
                }

                classToElementsSortedByDependencyLevel.putAll(clazz, elementsSortedByDependencyLevel);
            }

            else
            {
                classToElementsSortedByDependencyLevel.putAll(clazz, elementsByDependencyLevel);
            }
        });

        return classToElementsSortedByDependencyLevel;
    }

    private String getElementsInCircularDependency(MutableMap<String, MutableSet<String>> elementPrerequisites)
    {
        StringBuilder cycleBuilder = new StringBuilder();
        MutableSet<String> visitedElements = Sets.mutable.empty();
        MutableSet<String> stack = Sets.mutable.empty();
        MutableList<String> cycle = Lists.mutable.empty();

        for (String dependentElementFullPath : elementPrerequisites.keySet())
        {
            if (!visitedElements.contains(dependentElementFullPath))
            {
                if (dfs(cycleBuilder, elementPrerequisites, dependentElementFullPath, visitedElements, stack, cycle))
                {
                    break;
                }
            }
        }

        return cycleBuilder.toString();
    }

    private boolean dfs(StringBuilder cycleBuilder, MutableMap<String, MutableSet<String>> elementPrerequisites, String currentElementPath, MutableSet<String> visitedElements, MutableSet<String> stack, MutableList<String> cycle)
    {
        visitedElements.add(currentElementPath);
        stack.add(currentElementPath);
        cycle.add(currentElementPath);

        for (String prerequisiteFullPath : elementPrerequisites.getOrDefault(currentElementPath, Sets.mutable.empty()))
        {
            if (!visitedElements.contains(prerequisiteFullPath))
            {
                if (dfs(cycleBuilder, elementPrerequisites, prerequisiteFullPath, visitedElements, stack, cycle))
                {
                    return true;
                }
            }
            else if (stack.contains(prerequisiteFullPath))
            {
                int cycleStart = cycle.indexOf(prerequisiteFullPath);
                for (int i = cycleStart; i < cycle.size(); i++)
                {
                    cycleBuilder.append(cycle.get(i)).append(" -> ");
                }
                cycleBuilder.append(cycle.get(cycleStart));
                return true;
            }
        }

        stack.remove(currentElementPath);
        cycle.remove(cycle.size() - 1);
        return false;
    }

    public MutableMap<Class<? extends PackageableElement>, Collection<Class<? extends PackageableElement>>> getDependentToDependencies()
    {
        return dependentToDependencies == null ? Maps.fixedSize.empty() : dependentToDependencies;
    }

    public MutableMap<Class<? extends PackageableElement>, Collection<Class<? extends PackageableElement>>> getDependencyToDependents()
    {
        return dependencyToDependents;
    }

    public MutableList<MutableSet<Class<? extends PackageableElement>>> getTopologicallyOrderedClasses()
    {
        return topologicallyOrderedClasses;
    }

    public MutableSet<MutableSet<java.lang.Class<? extends PackageableElement>>> getDisjointDependencyGraphs()
    {
        constructDisjointDependencyGraphs();
        return disjointDependencyGraphs;
    }

    private void constructDisjointDependencyGraphs()
    {
        dependentToDependencies.forEach((dependent, dependencies) -> dependencies.forEach(dependency -> union(dependent, dependency)));
        MutableMap<Class<? extends PackageableElement>, MutableSet<Class<? extends PackageableElement>>> disjointDependencyGraphCollector = Maps.mutable.empty();
        unionFind.forEach((p, q) ->
        {
            Class<? extends PackageableElement> root = find(q);
            disjointDependencyGraphCollector.putIfAbsent(root, Sets.mutable.empty());
            disjointDependencyGraphCollector.get(root).add(p);
        });
        disjointDependencyGraphs.addAll(disjointDependencyGraphCollector.values());
    }

    private boolean connected(Class<? extends PackageableElement> p, Class<? extends PackageableElement> q)
    {
        return find(p) == find(q);
    }

    private Class<? extends PackageableElement> find(Class<? extends PackageableElement> p)
    {
        Class<? extends PackageableElement> root = p;
        while (root != unionFind.get(root))
        {
            root = unionFind.get(root);
        }

        while (p != root)
        {
            Class<? extends PackageableElement> next = unionFind.get(p);
            unionFind.put(p, root);
            p = next;
        }

        return root;
    }

    private void union(Class<? extends PackageableElement> p, Class<? extends PackageableElement> q)
    {
        if (connected(p, q))
        {
            return;
        }

        Class<? extends PackageableElement> root1 = find(p);
        Class<? extends PackageableElement> root2 = find(q);
        int root1Size = rankBySize.get(root1);
        int root2Size = rankBySize.get(root2);
        if (root1Size > root2Size)
        {
            unionFind.put(root2, root1);
            rankBySize.put(root1, root1Size + root2Size);
            rankBySize.put(root2, 0);
        }
        else
        {
            unionFind.put(root1, root2);
            rankBySize.put(root2, root1Size + root2Size);
            rankBySize.put(root1, 0);
        }
    }

    protected class PackageableElementsByDependencyLevel
    {
        private final FixedSizeList<Pair<PackageableElement, String>> independentElementAndPathPairs;

        protected PackageableElementsByDependencyLevel(Collection<Pair<PackageableElement, String>> packageableElementAndPathPairs)
        {
            this.independentElementAndPathPairs = Lists.fixedSize.withAll(packageableElementAndPathPairs);
        }

        protected FixedSizeList<Pair<PackageableElement, String>> getIndependentElementAndPathPairs()
        {
            return this.independentElementAndPathPairs;
        }
    }
}
