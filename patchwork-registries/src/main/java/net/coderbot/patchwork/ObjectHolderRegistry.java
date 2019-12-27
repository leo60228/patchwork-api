/*
 * Minecraft Forge, Patchwork Project
 * Copyright (c) 2016-2019, 2019
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.coderbot.patchwork;

import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.*;
import java.util.function.Consumer;

public class ObjectHolderRegistry {
	public static final ObjectHolderRegistry INSTANCE = new ObjectHolderRegistry();

	private HashMap<Registry<?>, ListenerList<?>> holders;

	private ObjectHolderRegistry() {
		holders = new HashMap<>();
	}

	@SuppressWarnings("unchecked")
	public <T> void register(Registry<T> registry, String namespace, String name, Consumer<T> consumer) {
		Identifier identifier = new Identifier(namespace, name);
		Optional<T> existing = registry.getOrEmpty(identifier);

		if (existing.isPresent()) {
			consumer.accept(existing.get());

			return;
		}

		ListenerList<T> list = (ListenerList<T>) holders.computeIfAbsent(registry, _registry -> new ListenerList<>(registry));

		list.listeners.computeIfAbsent(identifier, identifier_ -> new ArrayList<>()).add(consumer);
	}

	private static class ListenerList<T> implements RegistryEntryAddedCallback<T> {
		private Registry<T> registry;
		private Map<Identifier, List<Consumer<T>>> listeners;

		private ListenerList(Registry<T> registry) {
			this.registry = registry;
			this.listeners = new HashMap<>();

			RegistryEntryAddedCallback.event(registry).register(this);
		}

		@Override
		public void onEntryAdded(int i, Identifier identifier, T t) {
			List<Consumer<T>> consumers = listeners.get(identifier);

			if (consumers == null) {
				return;
			}

			T value = registry.get(identifier);

			for (Consumer<T> consumer : consumers) {
				consumer.accept(value);
			}
		}
	}
}
