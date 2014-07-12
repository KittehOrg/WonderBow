/*
 * * Copyright (C) 2014 Matt Baxter http://kitteh.org
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.kitteh.tenjava.jul2014;

import net.minecraft.server.v1_7_R3.PacketPlayOutWorldParticles;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

final class ParticleTimer {
    public enum Particle {
        ANGRY_VILLAGER("angryVillager"),
        HEART("heart", 10, 0),
        SPELL("spell", 50, 5),
        SPLASH("splash", 5, 0);

        private final int count;
        private final float data;
        private final String name;

        private Particle(String name) {
            this(name, 0, 1);
        }

        private Particle(String name, int count, float data) {
            this.name = name;
            this.count = count;
            this.data = data;
        }

        private float getData() {
            return this.data;
        }

        private int getCount() {
            return this.count;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private class ParticleTrack<T extends Entity> {
        private final Consumer<T> callback;
        private final T entity;
        private int count;
        private final int frequency;
        private final Particle particle;

        private ParticleTrack(T entity, int count, int frequency, Particle particle, Consumer<T> callback) {
            this.entity = entity;
            this.callback = callback;
            this.count = count;
            this.frequency = frequency;
            this.particle = particle;
        }

        private Consumer<T> getCallback() {
            return this.callback;
        }

        private T getEntity() {
            return this.entity;
        }

        private int count() {
            return count > 0 ? --count : count;
        }

        private int getFrequency() {
            return this.frequency;
        }

        private Particle getParticle() {
            return this.particle;
        }
    }

    private final WonderBow plugin;
    private final Random random = new Random();
    private final List<ParticleTrack> tracking = new LinkedList<>();
    private int tick = 0;

    public ParticleTimer(WonderBow plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                tick++;
                Iterator<ParticleTrack> iterator = tracking.iterator();
                while (iterator.hasNext()) {
                    ParticleTrack t = iterator.next();
                    if (!t.getEntity().isValid()) {
                        iterator.remove();
                        continue;
                    }
                    int frequency = t.getFrequency();
                    if (frequency <= 1 || tick % frequency == 0) {
                        broadcastEffect(t.getParticle().toString(), t.getEntity().getLocation(), t.getParticle().getData(), t.getParticle().getCount());
                        if (t.count() == 0) {
                            t.getCallback().accept(t.getEntity());
                            iterator.remove();
                        }
                    }
                }
            }
        }, 1, 1);
    }

    public <T extends Entity> void addEffect(Particle particle, T entity, int count, int frequency, Consumer<T> callback) {
        this.tracking.add(new ParticleTrack<>(entity, count, frequency, particle, callback));
    }

    private float off() {
        return random.nextFloat() * 2 + 1;
    }

    private void broadcastEffect(String name, Location location, float data, int count) {
        PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(name, (float) location.getX(), (float) location.getY(), (float) location.getZ(), off(), off(), off(), data, count);
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (player.getWorld().equals(location.getWorld()) && player.getLocation().distanceSquared(location) < (100 * 100)) {
                ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
            }
        }
    }
}