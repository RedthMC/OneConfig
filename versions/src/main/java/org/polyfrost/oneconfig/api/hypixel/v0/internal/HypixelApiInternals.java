/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.hypixel.v0.internal;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.handler.ClientboundPacketHandler;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPingPacket;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPlayerInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPingPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPlayerInfoPacket;
import net.hypixel.modapi.serializer.PacketSerializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.polyfrost.oneconfig.api.commands.v1.CommandManager;
import org.polyfrost.oneconfig.api.commands.v1.factories.builder.CommandBuilder;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.WorldLoadEvent;

import static org.polyfrost.oneconfig.api.commands.v1.factories.builder.CommandBuilder.runs;

@ApiStatus.Internal
public final class HypixelApiInternals {
    public static final HypixelApiInternals INSTANCE = new HypixelApiInternals();
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/HypixelAPI");
    private volatile NetHandlerPlayClient net;

    private HypixelApiInternals() {
        registerHypixelApi();
    }

    public static void init() {
        //<clinit>
    }


    private void registerHypixelApi() {
        HypixelModAPI.getInstance().setPacketSender((packet) -> {
            if (net == null) {
                if (Minecraft.getMinecraft().getNetHandler() != null) {
                    net = Minecraft.getMinecraft().getNetHandler();
                } else {
                    LOGGER.warn("dropping packet because no net handler is available");
                    return false;
                }
            }
            net.minecraft.network.PacketBuffer buf = new net.minecraft.network.PacketBuffer(Unpooled.buffer());
            packet.write(new PacketSerializer(buf));
            net.addToSendQueue(new net.minecraft.network.play.client.C17PacketCustomPayload(
                    //#if MC>12000
                    //$$ new net.minecraft.network.protocol.common.custom.DiscardedPayload(
                    //#endif
                        //#if MC<=11202
                        packet.getIdentifier(),
                        //#else
                        //$$ new net.minecraft.util.ResourceLocation(packet.getIdentifier()),
                        //#endif
                        buf
                    //#if MC>12000
                    //$$ )
                    //#endif
                )
            );
            return true;
        });

        HypixelModAPI.getInstance().registerHandler(new ClientboundPacketHandler() {
            @Override
            public void onHelloEvent(ClientboundHelloPacket packet) {
                System.out.println(packet);
            }

            @Override
            public void onLocationEvent(ClientboundLocationPacket packet) {
                System.out.println(packet);
            }

            @Override
            public void onPartyInfoPacket(ClientboundPartyInfoPacket packet) {
                System.out.println(packet);
            }

            @Override
            public void onPingPacket(ClientboundPingPacket packet) {
                System.out.println(packet);
            }

            @Override
            public void onPlayerInfoPacket(ClientboundPlayerInfoPacket packet) {
                System.out.println(packet);
            }
        });
        EventManager.register(WorldLoadEvent.class, (ev) -> {
            try {
                net = ev.manager;
                HypixelModAPI.getInstance().subscribeToEventPacket(ClientboundLocationPacket.class);
                Channel channel =
                    //#if FORGE
                    ev.manager.getNetworkManager().channel();
                    //#else
                    //$$ ((org.polyfrost.oneconfig.internal.mixin.fabric.ClientConnectionAccessor) ev.manager.
                        //#if MC<11300
                        //$$ getClientConnection()
                        //#else
                        //$$ getConnection()
                        //#endif
                    //$$ ).getChannel();
                    //#endif
                channel.pipeline().addBefore("packet_handler", "hypixel_mod_api_packet_handler", HypixelPacketHandler.INSTANCE);
            } catch (Exception e) {
                // already registered.
            }
        });

        CommandBuilder b = CommandManager.builder("hyp").description("hypixel api commands");
        b.then(runs("ping").does(() -> HypixelModAPI.getInstance().sendPacket(new ServerboundPingPacket())));
        b.then(runs("party").does(() -> HypixelModAPI.getInstance().sendPacket(new ServerboundPartyInfoPacket())));
        b.then(runs("player").does(() -> HypixelModAPI.getInstance().sendPacket(new ServerboundPlayerInfoPacket())));
        CommandManager.registerCommand(b);
    }

    @ChannelHandler.Sharable
    private static class HypixelPacketHandler extends SimpleChannelInboundHandler<Packet<?>> {
        private static final HypixelPacketHandler INSTANCE = new HypixelPacketHandler();

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Packet<?> msg) {
            ctx.fireChannelRead(msg);

            if (!(msg instanceof net.minecraft.network.play.server.S3FPacketCustomPayload)) {
                return;
            }

            net.minecraft.network.play.server.S3FPacketCustomPayload packet = (net.minecraft.network.play.server.S3FPacketCustomPayload) msg;
            // reason: needed for 1.16+
            //noinspection StringOperationCanBeSimplified
            String identifier = packet.getChannelName().toString();
            if (!HypixelModAPI.getInstance().getRegistry().isRegistered(identifier)) {
                return;
            }

            try {
                HypixelModAPI.getInstance().handle(identifier, new PacketSerializer(packet.getBufferData()));
            } catch (Exception e) {
                LOGGER.warn("Failed to handle packet {}", identifier, e);
            }
        }
    }
}
