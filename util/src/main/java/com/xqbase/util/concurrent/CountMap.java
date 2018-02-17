package com.xqbase.util.concurrent;

import java.util.concurrent.ConcurrentHashMap;

public class CountMap<K> extends ConcurrentHashMap<K, Count> {
	private static final long serialVersionUID = 1L;

	protected Count newCount() {
		return new Count();
	}

	public Count acquire(K key) {
		Count count = computeIfAbsent(key, k -> newCount());
		count.incrementAndGet();
		return count;
	}

	public void release(K key, Count count) {
		if (count.decrementAndGet() == 0) {
			remove(key, Count.ZERO);
		}
	}
}