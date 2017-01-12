/*
 * The MIT License
 *
 * Copyright (c) 2013 IKEDA Yasuyuki
 * This class originates from the class with the same name in flexible publish plugin:
 * https://github.com/jenkinsci/flexible-publish-plugin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.conditionalbuildstep.dependency;

import java.util.List;
import java.util.Set;

import org.jenkins_ci.plugins.run_condition.RunCondition;
import org.jenkins_ci.plugins.run_condition.BuildStepRunner;

import hudson.model.AbstractProject;
import hudson.model.DependencyGraph;

/**
 * Wraps {@link DependencyGraph} and append {@link RunCondition} to {@link Dependency}.
 *
 * Methods other than addDependency are just calling methods of wrapped {@link DependencyGraph}
 */
public class ConditionalDependencyGraphWrapper extends DependencyGraph
{
    private DependencyGraph graph;
    private RunCondition condition;
    private BuildStepRunner runner;

    public ConditionalDependencyGraphWrapper(DependencyGraph graph, RunCondition condition, BuildStepRunner runner) {
        this.graph = graph;
        this.condition = condition;
        this.runner = runner;
    }

    /**
     * Add dependency. {@link RunCondition} will be attached.
     *
     * @see hudson.model.DependencyGraph#addDependency(hudson.model.DependencyGraph.Dependency)
     */
    @Override
    public void addDependency(Dependency dep) {
        graph.addDependency(new ConditionalDependencyWrapper(dep, condition, runner));
    }

    @Override
    public void build() {
        graph.build();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public int compare(AbstractProject o1, AbstractProject o2) {
        return graph.compare(o1, o2);
    }

    @Override
    public <T> T getComputationalData(Class<T> key) {
        return graph.getComputationalData(key);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<AbstractProject> getDownstream(AbstractProject p) {
        return graph.getDownstream(p);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<Dependency> getDownstreamDependencies(AbstractProject p) {
        return graph.getDownstreamDependencies(p);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Set<AbstractProject> getTransitiveDownstream(AbstractProject src) {
        return graph.getTransitiveDownstream(src);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Set<AbstractProject> getTransitiveUpstream(AbstractProject src) {
        return graph.getTransitiveUpstream(src);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<AbstractProject> getUpstream(AbstractProject p) {
        return graph.getUpstream(p);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<Dependency> getUpstreamDependencies(AbstractProject p) {
        return graph.getUpstreamDependencies(p);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean hasIndirectDependencies(AbstractProject src, AbstractProject dst) {
        return graph.hasIndirectDependencies(src, dst);
    }

    @Override
    public <T> void putComputationalData(Class<T> key, T value) {
        graph.putComputationalData(key, value);
    }
}
