package org.origins_yggdrasil.yggdrasil_origins.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.origins_yggdrasil.yggdrasil_origins.YggdrasilOrigins;

/** Packets of our own class selection system (Origins is no longer needed for it). */
public final class YggPackets {

    private YggPackets() { }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(YggdrasilOrigins.MOD_ID, path);
    }

    /** Server -> client: registry snapshot (classes, layers) plus the player choice, as JSON. */
    public record Sync(String json) implements CustomPacketPayload {

        public static final Type<Sync> TYPE = new Type<>(id("sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(1_000_000), Sync::json, Sync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: player looks (proportions and parts), as JSON. */
    public record Visuals(String json) implements CustomPacketPayload {

        public static final Type<Visuals> TYPE = new Type<>(id("visuals"));
        public static final StreamCodec<FriendlyByteBuf, Visuals> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256_000), Visuals::json, Visuals::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: bar state (cooldowns and resources) for the HUD, as JSON. */
    public record Hud(String json) implements CustomPacketPayload {

        public static final Type<Hud> TYPE = new Type<>(id("hud"));
        public static final StreamCodec<FriendlyByteBuf, Hud> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64_000), Hud::json, Hud::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: open the picker screen for a layer. */
    public record Open(String layer) implements CustomPacketPayload {

        public static final Type<Open> TYPE = new Type<>(id("open"));
        public static final StreamCodec<FriendlyByteBuf, Open> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Open::layer, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: "back" - re-pick the previous layer. */
    public record Back(String layer) implements CustomPacketPayload {

        public static final Type<Back> TYPE = new Type<>(id("back"));
        public static final StreamCodec<FriendlyByteBuf, Back> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Back::layer, Back::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: the player choice. */
    /** Client -> server: the look a player settled on, as compact JSON. */
    public record Style(String json) implements CustomPacketPayload {

        public static final Type<Style> TYPE = new Type<>(id("style"));
        public static final StreamCodec<FriendlyByteBuf, Style> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32_000), Style::json, Style::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: open the customisation step. */
    public record Customise(boolean forced) implements CustomPacketPayload {

        public static final Type<Customise> TYPE = new Type<>(id("customise"));
        public static final StreamCodec<FriendlyByteBuf, Customise> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, Customise::forced, Customise::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Choose(String layer, String clazz) implements CustomPacketPayload {

        public static final Type<Choose> TYPE = new Type<>(id("choose"));
        public static final StreamCodec<FriendlyByteBuf, Choose> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Choose::layer,
            ByteBufCodecs.STRING_UTF8, Choose::clazz,
            Choose::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
