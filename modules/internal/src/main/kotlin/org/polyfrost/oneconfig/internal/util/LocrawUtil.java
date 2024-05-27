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

/*
 * Co-author: lyndseyy (Lyndsey Winter) <https://github.com/lyndseyy>
 * Co-author: asbyth <cyronize@gmail.com> (deleted GitHub account)
 * Co-author: Moire9 (Moiré) <https://github.com/Moire9> (non-copyrightable contribution)
 * Co-author: Sk1er (Mitchell Katz) <https://github.com/Sk1er>
 * Co-author: Cubxity <https://github.com/Cubxity> (non-copyrightable contribution)
 * Co-author: UserTeemu <https://github.com/UserTeemu> (non-copyrightable contribution)
 * Co-author: PyICoder (Befell) <https://github.com/PyICoder> (non-copyrightable contribution)
 */

package org.polyfrost.oneconfig.internal.util;

import com.google.gson.Gson;
import org.polyfrost.oneconfig.api.event.v1.EventDelay;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ChatReceiveEvent;
import org.polyfrost.oneconfig.api.event.v1.events.ChatSendEvent;
import org.polyfrost.oneconfig.api.event.v1.events.LocrawEvent;
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent;
import org.polyfrost.oneconfig.api.event.v1.events.WorldLoadEvent;
import org.polyfrost.oneconfig.api.event.v1.invoke.EventHandler;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.utils.v1.hypixel.HypixelUtils;
import org.polyfrost.oneconfig.utils.v1.hypixel.LocrawInfo;
import org.polyfrost.universal.UChat;

public final class LocrawUtil {
    public static final LocrawUtil INSTANCE = new LocrawUtil();
    private static final Gson GSON = new Gson();

    private LocrawInfo locrawInfo;
    private LocrawInfo lastLocrawInfo;
    private boolean listening;
    private int tick;
    private boolean playerSentCommand = false;

    public static void init() {
        // ; <cl-init>
    }

    private LocrawUtil() {
        EventHandler.of(TickEvent.End.class, (event) -> {
            if (!Platform.getPlayerPlatform().doesPlayerExist() || !HypixelUtils.isHypixel()) {
                return;
            }
            this.tick++;
            if (this.tick == 40 || this.tick % 520 == 0) {
                sendLocraw(false);
            }
        }).register();

        EventHandler.of(WorldLoadEvent.class, (event) -> {
            if (locrawInfo != null) {
                lastLocrawInfo = locrawInfo;
            }
            locrawInfo = null;
            tick = 0;
        }).register();

        EventHandler.of(ChatSendEvent.class, (event) -> {
            if (event.message.startsWith("/locraw") && !this.listening) {
                playerSentCommand = true;
            }
        }).register();

        EventHandler.of(ChatReceiveEvent.class, this::onMessageReceived).register();
    }

    private void sendLocraw(boolean delay) {
        EventDelay.tick((delay ? 20 : 0), () -> {
            this.listening = true;
            UChat.say("/locraw");
        });
    }

    private void onMessageReceived(ChatReceiveEvent event) {
        if (HypixelUtils.isHypixel()) return;
        try {
            // Had some false positives while testing, so this is here just to be safe.
            final String msg = event.getFullyUnformattedMessage();
            if (msg.startsWith("You are sending too many commands! Please try again in a few seconds.")) {
                sendLocraw(true);
                return;
            }
            if (msg.startsWith("{")) {
                // Parse the json, and make sure that it's not null.
                LocrawInfo locrawInfo = GSON.fromJson(msg, LocrawInfo.class);
                if (locrawInfo != null) {
                    // Gson does not want to parse the GameType, as some stuff is different so this
                    // is just a way around that to make it properly work :)
                    locrawInfo.previous = lastLocrawInfo;

                    // Stop listening for locraw and cancel the message.
                    if (!this.playerSentCommand) {
                        event.cancelled = true;
                    }
                    EventManager.INSTANCE.post(new LocrawEvent(locrawInfo));

                    this.playerSentCommand = false;
                    this.listening = false;
                }
            }
        } catch (Exception ignored) {
        }
    }
}
