/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.platform.impl;

import cc.polyfrost.oneconfig.libs.universal.UGraphics;
import cc.polyfrost.oneconfig.libs.universal.UMatrixStack;
import cc.polyfrost.oneconfig.libs.universal.UMinecraft;
import cc.polyfrost.oneconfig.platform.GLPlatform;
import cc.polyfrost.oneconfig.renderer.TextRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import java.util.regex.Pattern;

//#if FORGE==1
import net.minecraft.client.shader.Framebuffer;
//#else
//$$ import cc.polyfrost.oneconfig.internal.hook.FramebufferHook;
//#endif

public class GLPlatformImpl implements GLPlatform {
    private static final Pattern regex = Pattern.compile("(?i)\u00A7[0-9a-f]");

    private int drawBorderedText(String text, float x, float y, int color, int opacity) {
        String noColors = regex.matcher(text).replaceAll("\u00A7r");
        int yes = 0;
        if (opacity / 4 > 3) {
            for (int xOff = -2; xOff <= 2; xOff++) {
                for (int yOff = -2; yOff <= 2; yOff++) {
                    if (xOff * xOff != yOff * yOff) {
                        yes += drawText(
                            noColors, (xOff / 2f) + x, (yOff / 2f) + y, (opacity / 4) << 24, false
                        );
                    }
                }
            }
        }
        yes += (int) drawText(text, x, y, color, false);
        return yes;
    }

    @Override
    public void drawRect(float x, float y, float x2, float y2, int color) {
        if (x < x2) {
            float i = x;
            x = x2;
            x2 = i;
        }

        if (y < y2) {
            float i = y;
            y = y2;
            y2 = i;
        }

        float f = (float)(color >> 24 & 0xFF) / 255.0F;
        float g = (float)(color >> 16 & 0xFF) / 255.0F;
        float h = (float)(color >> 8 & 0xFF) / 255.0F;
        float j = (float)(color & 0xFF) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        UGraphics.enableBlend();
        //noinspection deprecation
        UGraphics.disableTexture2D();
        UGraphics.tryBlendFuncSeparate(770, 771, 1, 0);
        UGraphics.color4f(g, h, j, f);
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(x, y2, 0.0).endVertex();
        worldRenderer.pos(x2, y2, 0.0).endVertex();
        worldRenderer.pos(x2, y, 0.0).endVertex();
        worldRenderer.pos(x, y, 0.0).endVertex();
        tessellator.draw();
        //noinspection deprectation
        UGraphics.enableTexture2D();
        UGraphics.disableBlend();
    }

    @Override
    public void enableStencil() {
        //#if FORGE==1
        Framebuffer framebuffer = UMinecraft.getMinecraft().getFramebuffer();
        //#else
        //$$ FramebufferHook framebuffer = ((FramebufferHook) UMinecraft.getMinecraft().getFramebuffer());
        //#endif
        if (!framebuffer.isStencilEnabled()) {
            framebuffer.enableStencil();
        }
    }
    @Override
    public float drawText(String text, float x, float y, int color, TextRenderer.TextType type) {
        switch (type) {
            case NONE:
                return drawText(text, x, y, color, false);
            case SHADOW:
                return drawText(text, x, y, color, true);
            case FULL:
                //#if FORGE==1 && MC<=11202
                return cc.polyfrost.oneconfig.internal.renderer.BorderedTextHooks.INSTANCE.drawString(text, x, y, color, type);
                //#else
                //$$ return drawBorderedText(text, x, y, color, 255);
                //#endif
        }
        return 0f;
    }

    @Override
    public float drawText(UMatrixStack matrixStack, String text, float x, float y, int color, boolean shadow) {
        //#if MC<=11202
        return UMinecraft.getFontRenderer().drawString(text, x, y, color, shadow);
        //#else
        //$$ if(shadow) {
        //$$    return UMinecraft.getFontRenderer().drawStringWithShadow(matrixStack.toMC(), text, x, y, color);
        //$$ } else return UMinecraft.getFontRenderer().drawString(matrixStack.toMC(), text, x, y, color);
        //#endif
    }

    @Override
    public int getStringWidth(String text) {
        return UMinecraft.getFontRenderer().getStringWidth(text);
    }
}
