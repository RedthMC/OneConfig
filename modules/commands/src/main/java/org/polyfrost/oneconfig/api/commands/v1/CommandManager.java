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

package org.polyfrost.oneconfig.api.commands.v1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.polyfrost.oneconfig.api.commands.v1.arguments.ArgumentParser;
import org.polyfrost.oneconfig.api.commands.v1.factories.CommandFactory;
import org.polyfrost.oneconfig.api.commands.v1.factories.PlatformCommandFactory;
import org.polyfrost.oneconfig.api.commands.v1.factories.annotated.AnnotationCommandFactory;
import org.polyfrost.oneconfig.api.commands.v1.factories.annotated.Command;
import org.polyfrost.oneconfig.api.commands.v1.factories.builder.BuilderFactory;
import org.polyfrost.oneconfig.api.commands.v1.factories.builder.CommandBuilder;
import org.polyfrost.oneconfig.api.commands.v1.factories.dsl.CommandDSL;
import org.polyfrost.oneconfig.api.commands.v1.factories.dsl.DSLFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Handles the registration of OneConfig commands.
 *
 * @see Command
 */
public class CommandManager {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Commands");
    /**
     * The singleton instance of the command manager.
     */
    public static final CommandManager INSTANCE = new CommandManager();
    private static final PlatformCommandFactory platform;
    /**
     * use {@link #registerParser(ArgumentParser)} to register a parser
     */
    @ApiStatus.Internal
    public final Map<Class<?>, ArgumentParser<?>> parsers = new HashMap<>();
    private final Set<CommandFactory> factories = new HashSet<>();

    static {
        PlatformCommandFactory p;
        try {
            p = ServiceLoader.load(PlatformCommandFactory.class, PlatformCommandFactory.class.getClassLoader()).iterator().next();
        } catch (Throwable t) {
            LOGGER.error("failed to load platform command manager!", t);
            p = null;
        }
        platform = p;
    }

    private CommandManager() {
        parsers.putAll(ArgumentParser.defaultParsers);
        registerFactory(new DSLFactory());
        registerFactory(new AnnotationCommandFactory());
        registerFactory(new BuilderFactory());
    }

    public static CommandDSL dsl(String... aliases) {
        return new CommandDSL(INSTANCE.parsers, aliases);
    }

    public static CommandBuilder builder(String... aliases) {
        return new CommandBuilder(INSTANCE.parsers, aliases);
    }

    public static boolean registerCommand(Object obj) {
        return INSTANCE.create(obj);
    }

    /**
     * Register a factory which can be used to create commands from objects in the {@link #create(Object)} method.
     */
    public void registerFactory(CommandFactory factory) {
        factories.add(factory);
    }

    /**
     * Register a parser which can be used to parse arguments needed by commands.
     */
    public void registerParser(ArgumentParser<?> parser) {
        parsers.put(parser.getType(), parser);
    }

    public void registerParser(ArgumentParser<?>... parsers) {
        for (ArgumentParser<?> p : parsers) {
            registerParser(p);
        }
    }

    /**
     * Create a command from the provided object.
     * <br>
     * The details of this process are down to the registered {@link CommandFactory} instances.
     *
     * @return true if a command was created, false otherwise (meaning no factory was able to process the given object into a command)
     */
    public boolean create(Object obj) {
        return createTree(obj) != null;
    }

    /**
     * Create a command from the given object
     *
     * @see #create(Object)
     */
    public CommandTree createTree(Object obj) {
        for (CommandFactory f : factories) {
            CommandTree t = f.create(parsers, obj);
            if (t != null) {
                t.init();
                if (platform != null) platform.createCommand(t);
                else LOGGER.warn("didn't create command with platform as it is missing (check logs)");
                return t;
            }
        }
        return null;
    }
}