/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 * Peter Smith
 *******************************************************************************/
package org.boris.expr.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Graph implements Iterable<Object> {
	private boolean wantsEdges = true;
	private Set<Object> nodes = new HashSet<>();
	private Set<Object> edges = new HashSet<>();
	private Map<Object, Set<Object>> outbounds = new HashMap<>();
	private Map<Object, Set<Object>> inbounds = new HashMap<>();
	private List<Object> ordered = null;
	private Set<Object> traversed = null;

	public void setIncludeEdges(boolean include) {
		this.wantsEdges = include;
	}

	public void add(Object node) {
		nodes.add(node);
	}

	public Set<Object> getInbounds(Object node) {
		return inbounds.get(node);
	}

	public Set<Object> getOutbounds(Object node) {
		return outbounds.get(node);
	}

	void clearOutbounds(Object node) {
		Set<Object> s = outbounds.get(node);
		if (s != null) {
			for (Object value : s)
				remove((Edge) value);
		}
	}

	public void clearInbounds(Object node) {
		Set<Object> s = inbounds.get(node);
		if (s != null) {
			int previousSize = s.size();
			while (!s.isEmpty()) {
				// Iterator<Object> i = s.iterator();
				// while (i.hasNext())
				remove((Edge) s.iterator().next());
				if (s.size() == previousSize)
					break;
				previousSize = s.size();
			}
		}
	}

	public void remove(Object node) {
		nodes.remove(node);
		clearInbounds(node);
		clearOutbounds(node);
	}

	public void add(Edge e) {
		// checkCycle(e); FIXME THE ALGORITHM USED BY checkCycle is wrong and detects spurious cycles
		nodes.add(e.source);
		nodes.add(e.target);
		edges.add(e);
		Set<Object> in = inbounds.get(e.target);
		if (in == null)
			inbounds.put(e.target, in = new HashSet<>());
		in.add(e);
		Set<Object> out = outbounds.get(e.source);
		if (out == null)
			outbounds.put(e.source, out = new HashSet<>());
		out.add(e);
	}

	public void checkCycle(Edge e) throws GraphCycleException {
		HashSet<Object> visited = new HashSet<>();
		visited.add(e.source);
		checkCycle(e, visited);
	}

	private void checkCycle(Edge e, HashSet<Object> visited) throws GraphCycleException {
		if (visited.contains(e.target)) {
			throw new GraphCycleException("Circular reference found: " + e.source + " - " + e.target);
		}
		visited.add(e.target);
		Set<Object> out = outbounds.get(e.target);
		if (out != null) {
			for (Object anOut : out) {
				checkCycle((Edge) anOut, visited);
			}
		}
	}

	public void remove(Edge e) {
		edges.remove(e);
		Set<Object> in = inbounds.get(e.target);
		if (in != null)
			in.remove(e);
		Set<Object> out = outbounds.get(e.source);
		if (out != null)
			out.remove(e);
	}

	public void sort() {
		ordered = new ArrayList<>();
		traversed = new HashSet<>();
		Iterator<Object> i = nodes.iterator();
		Set<Object> remains = new HashSet<>(nodes);

		// First traverse nodes without inbounds
		while (i.hasNext()) {
			Object o = i.next();
			Set<Object> in = inbounds.get(o);
			if (in == null || in.isEmpty()) {
				traverse(o);
				remains.remove(o);
			}
		}

		// Now traverse the rest
		i = remains.iterator();
		while (i.hasNext()) {
			Object o = i.next();
			if (!traversed.contains(o)) {
				traverse(o);
			}
		}
	}

	private void traverse(Object node) {
		Set<Object> in = inbounds.get(node);
		if (in != null) {

			// if all inbounds haven't been traversed we must stop
			for (Object anIn : in) {
				Edge e = (Edge) anIn;
				if (!traversed.contains(e.source))
					return;
				else if (wantsEdges)
					ordered.add(e);

			}
		}

		if (!traversed.contains(node)) {
			traversed.add(node);
			ordered.add(node);
		}

		Set<Object> out = outbounds.get(node);
		if (out == null || out.isEmpty()) {
			return;
		}

		Set<Object> avoid = new HashSet<>();

		Iterator<Object> i = out.iterator();
		while (i.hasNext()) {
			Edge e = (Edge) i.next();
			if (!traversed.contains(e)) {
				if (traversed.contains(e.target)) {
					avoid.add(e.target);
				}
			}
		}

		i = out.iterator();
		while (i.hasNext()) {
			Object n = ((Edge) i.next()).target;
			if (!avoid.contains(n)) {
				traverse(n);
			}
		}
	}

	public void clear() {
		edges.clear();
		inbounds.clear();
		outbounds.clear();
		nodes.clear();
		traversed = null;
		ordered.clear();
	}

	@Override
	public Iterator<Object> iterator() {
		if (ordered == null)
			sort();
		return ordered.iterator();
	}

	public void traverse(Object node, GraphTraversalListener listener) {
		HashSet<Object> subgraph = new HashSet<>();
		walk(node, subgraph);
		HashSet<Object> hs = new HashSet<>();
		hs.add(node);
		traverse(node, listener, hs, subgraph);
	}

	private void walk(Object node, Set<Object> traversed) {
		traversed.add(node);
		Set<Object> out = outbounds.get(node);
		if (out != null) {
			for (Object anOut : out) {
				Edge e = (Edge) anOut;
				walk(e.target, traversed);
			}
		}
	}

	private void traverse(Object node, GraphTraversalListener listener, Set<Object> traversed, Set<Object> subgraph) {
		Set<Object> edges = outbounds.get(node);
		if (edges != null) {
			for (Object edge : edges) {
				Edge e = (Edge) edge;
				Set<Object> ins = inbounds.get(e.target);
				Iterator<Object> j = ins.iterator();
				boolean traverse = true;
				while (j.hasNext()) {
					Edge in = (Edge) j.next();
					if (subgraph.contains(in.source) && !traversed.contains(in.source) && !node.equals(in.source)) {
						traverse = false;
						break;
					}
				}
				if (traverse) {
					listener.traverse(e.target);
					traversed.add(e.target);
					traverse(e.target, listener, traversed, subgraph);
				}
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Object edge : edges) {
			sb.append(edge);
			sb.append("\n");
		}
		return sb.toString();
	}
}
