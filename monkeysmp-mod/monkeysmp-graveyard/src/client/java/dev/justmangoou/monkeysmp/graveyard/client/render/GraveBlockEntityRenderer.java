package dev.justmangoou.monkeysmp.graveyard.client.render;

import java.util.HashMap;
import java.util.Map;

import dev.justmangoou.monkeysmp.graveyard.block.entity.GraveBlockEntity;
import dev.justmangoou.monkeysmp.graveyard.config.MonkeySMPGraveyardConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.justmangoou.monkeysmp.graveyard.client.events.RenderGlowingGraveEvent;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Block entity rendering moved from an immediate-mode render(entity, ..., PoseStack, MultiBufferSource, ...) call
 * to a two-phase extract/submit model: extractRenderState() runs on the main thread and copies whatever the
 * renderer needs out of the live GraveBlockEntity into an immutable-ish GraveRenderState; submit() later renders
 * using only that state (no BlockEntity access) via a SubmitNodeCollector, which replaces direct VertexConsumer
 * writes with submitModelPart/submitText/etc. calls. Outline/glow rendering is now a plain int color parameter
 * on those submit calls instead of routing through a separate OutlineBufferSource, so the old LevelRendererMixin
 * hack (forcing block entities through the outline render pass) and its accessor are no longer needed and were
 * removed.
 *
 * Simplification: the "adaptRenderer" ground-matches-underlying-block visual (rendering the block below the
 * grave onto the ground cuboid) is dropped. Replicating it needs BlockModelResolver/BlockStateModelPart, which
 * is a separate, large piece of the same rendering rewrite; the ground cuboid now always renders with its
 * configured texture regardless of that setting.
 */
public class GraveBlockEntityRenderer implements BlockEntityRenderer<GraveBlockEntity, GraveBlockEntityRenderer.GraveRenderState> {
	private static final Gson GSON = new Gson();
	private static final int OUTLINE_COLOR = 0xFFFFFFFF;

	private final Font textRenderer;
	private final Minecraft client;
	private final SpriteGetter sprites;
	private final PlayerSkinRenderCache playerSkinRenderCache;
	private final SkullModelBase skullModel;

	private static ModelPart graveModel;
	@Nullable
	private static TextRenderInfo textRenderInfo = null;
	@Nullable
	private static SkullRenderInfo skullRenderInfo = null;
	private static final Map<String, SpriteId> CUBOID_SPRITES = new HashMap<>();

	public static boolean syncedGlowing = true;
	public static int syncedGlowingMaxDistance = Integer.MAX_VALUE;
	public static double syncedDeathSightDistance = Integer.MAX_VALUE;

	public GraveBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
		this.textRenderer = context.font();
		this.client = Minecraft.getInstance();
		this.sprites = context.sprites();
		this.playerSkinRenderCache = context.playerSkinRenderCache();
		this.skullModel = SkullBlockRenderer.createModel(context.entityModelSet(), SkullBlock.Types.PLAYER);
	}

	@Override
	public GraveRenderState createRenderState() {
		return new GraveRenderState();
	}

	@Override
	public void extractRenderState(GraveBlockEntity entity, GraveRenderState state, float partialTicks, Vec3 cameraPosition, @Nullable CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(entity, state, partialTicks, cameraPosition, breakProgress);

		MonkeySMPGraveyardConfig.GraveRendering config = MonkeySMPGraveyardConfig.getConfig().graveRendering;
		state.config = config;
		state.direction = entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
		state.skullOwner = entity.getGraveSkull();
		state.graveText = entity.getGraveText();

		LocalPlayer player = this.client.player;
		state.glowing = config.useGlowingEffect && entity.isUnclaimed()
				&& RenderGlowingGraveEvent.EVENT.invoker().canRenderOutline(entity, player);
	}

	@Override
	public void submit(GraveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		MonkeySMPGraveyardConfig.GraveRendering config = state.config;
		if (!config.useCustomFeatureRenderer) return;

		float rotation = (float) switch (state.direction) {
			case SOUTH -> Math.PI;
			case WEST -> Math.PI * 0.5D;
			case EAST -> Math.PI * 1.5D;
			default -> 0; // North (can't be up/down)
		};

		poseStack.pushPose();
		poseStack.rotateAround(Axis.YP.rotation(rotation), .5f, .5f, .5f);

		int outlineColor = state.glowing ? OUTLINE_COLOR : 0;

		if (config.useSkullRenderer && state.skullOwner != null)
			this.submitOwnerSkull(state, poseStack, collector, outlineColor);
		if (config.useTextRenderer && state.graveText != null)
			this.submitGraveText(state, poseStack, collector);
		this.submitGraveModel(state, poseStack, collector, outlineColor);

		poseStack.popPose();
	}

	private void submitOwnerSkull(GraveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, int outlineColor) {
		RenderType renderType = this.playerSkinRenderCache.getOrDefault(state.skullOwner).renderType();
		this.submitSkull(state, poseStack, collector, renderType, outlineColor);
	}

	/**
	 * Submit the skull model with a given RenderType, so it can be submitted both with the skin texture and (when glowing) an outline.
	 */
	private void submitSkull(GraveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType, int outlineColor) {
		if (skullRenderInfo == null) return;

		poseStack.pushPose();
		poseStack.translate(0.5f, 0.25f, 0.5f); // Required for calculations of rotation and scale to not change position

		int[] rotation = skullRenderInfo.rotation;

		poseStack.translate(0D, -(4 - skullRenderInfo.height) / 16D, -(8 - skullRenderInfo.depth) / 16D);

		org.joml.Quaternionf angle = new org.joml.Quaternionf().rotateXYZ((float) Math.toRadians(rotation[0]), (float) Math.toRadians(rotation[1]), (float) Math.toRadians(rotation[2]));
		poseStack.mulPose(angle);
		poseStack.scale(skullRenderInfo.scaleFace, skullRenderInfo.scaleFace, skullRenderInfo.scaleDepth);

		poseStack.translate(-0.5f, -0.25f, -0.5f); // Move back to actual position. Calculations of scale and rotation are now done

		SkullBlockRenderer.submitSkull(0F, poseStack, collector, state.lightCoords, this.skullModel, renderType, outlineColor, null);
		poseStack.popPose();
	}

	private void submitGraveText(GraveRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
		Component graveText = state.graveText;
		if (textRenderInfo == null) return;

		poseStack.pushPose();

		poseStack.translate(.5, textRenderInfo.height / 16f, textRenderInfo.depth / 16f - 0.0001f);
		poseStack.scale(-1, -1, 0);

		int textWidth = this.textRenderer.width(graveText.getString());
		float scale = textRenderInfo.width / (textWidth * 16f);
		poseStack.scale(scale, scale, scale);

		poseStack.translate(-textWidth / 2.0, -4.5, 0);

		collector.submitText(poseStack, 0f, 0f, graveText.getVisualOrderText(), false, Font.DisplayMode.NORMAL, state.lightCoords, 0xFFFFFF, 0x0, 0);

		poseStack.popPose();
	}

	private void submitGraveModel(GraveRenderState state, PoseStack poseStack, SubmitNodeCollector collector, int outlineColor) {
		for (Map.Entry<String, SpriteId> cuboid : CUBOID_SPRITES.entrySet()) {
			SpriteId spriteId = cuboid.getValue();
			ModelPart part = graveModel.getChild(cuboid.getKey());

			RenderType renderType = spriteId.renderType(RenderTypes::entityCutout);
			TextureAtlasSprite sprite = this.sprites.get(spriteId);

			collector.submitModelPart(part, poseStack, renderType, state.lightCoords, OverlayTexture.NO_OVERLAY, sprite, -1, null, outlineColor);
		}
	}

	/**
	 * Takes JSON and reloads current grave model to what the JSON describes
	 * @param json Model json (same the baked block model uses)
	 * @throws IllegalStateException if the model json is incomplete or wrong
	 */
	public static void reloadModelFromJson(JsonObject json) throws IllegalStateException {
		CUBOID_SPRITES.clear();
		MeshDefinition modelData = new MeshDefinition();
		PartDefinition root = modelData.getRoot();

		JsonArray textureSize = json.getAsJsonArray("texture_size");
		JsonObject textures = json.getAsJsonObject("textures");
		JsonArray elements = json.getAsJsonArray("elements");
		JsonObject features = json.has("features") ? json.getAsJsonObject("features") : null;

		int uvX = textureSize.get(0).getAsInt();
		int uvY = textureSize.get(1).getAsInt();
		Map<String, String> nameIds = new HashMap<>();
		for (Map.Entry<String, JsonElement> e : textures.entrySet()) {
			String key = e.getKey();
			String value = e.getValue().getAsString();

			nameIds.put(key, value);
		}
		int i = 0;
		for (JsonElement e : elements) {
			JsonObject o = e.getAsJsonObject();
			String name = o.has("name") ? o.get("name").getAsString() : String.valueOf(i++);
			JsonArray from = o.getAsJsonArray("from");
			JsonArray to = o.getAsJsonArray("to");
			JsonObject faces = o.getAsJsonObject("faces");

			float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
			String textureName = "";
			for (Map.Entry<String, JsonElement> face : faces.entrySet()) {
				JsonObject value = face.getValue().getAsJsonObject();
				JsonArray uv = value.getAsJsonArray("uv");
				textureName = value.get("texture").getAsString();

				minX = Math.min(minX, uv.get(0).getAsFloat());
				minY = Math.min(minY, uv.get(1).getAsFloat());
			}
			minX *= uvX / 16f;
			minY *= uvY / 16f;

			textureName = textureName.replaceFirst("#", "");
			if (nameIds.containsKey(textureName)) {
				textureName = nameIds.get(textureName);
			}
			Identifier texture = Identifier.parse(textureName);
			SpriteId spriteId = new SpriteId(TextureAtlas.LOCATION_BLOCKS, texture);

			CUBOID_SPRITES.put(name, spriteId);

			float fromX = from.get(0).getAsFloat();
			float fromY = from.get(1).getAsFloat();
			float fromZ = from.get(2).getAsFloat();
			float toX = to.get(0).getAsFloat();
			float toY = to.get(1).getAsFloat();
			float toZ = to.get(2).getAsFloat();

			// Min no longer have to be in from
			float lowerX = Math.min(fromX, toX);
			float lowerY = Math.min(fromY, toY);
			float lowerZ = Math.min(fromZ, toZ);
			float higherX = Math.max(fromX, toX);
			float higherY = Math.max(fromY, toY);
			float higherZ = Math.max(fromZ, toZ);
			addChildPart(root, name, (int) minX, (int) minY, lowerX, lowerY, lowerZ, higherX - lowerX, higherY - lowerY, higherZ - lowerZ);
		}
		if (features != null) {
			if (features.has("text")) {
				textRenderInfo = GSON.fromJson(features.get("text"), TextRenderInfo.class);
			}
			if (features.has("skull")) {
				skullRenderInfo = GSON.fromJson(features.get("skull"), SkullRenderInfo.class);
			}
		}
		graveModel = LayerDefinition.create(modelData, uvX, uvY).bakeRoot();
	}
	private static ModelPart getGraveModel() {
		MeshDefinition modelData = new MeshDefinition();
		PartDefinition root = modelData.getRoot();
		addChildPart(root, "ground", 0, 0, 0, 0, 0, 16, 1, 16);
		addChildPart(root, "base", 0, 21, 2, 1, 10, 12, 2, 5);
		addChildPart(root, "bust", 0, 28, 3, 3, 11, 10, 12, 3);
		addChildPart(root, "top", 0, 17, 4, 15, 11, 8, 1, 3);

		return LayerDefinition.create(modelData, 64, 64).bakeRoot();
	}
	private static void addChildPart(PartDefinition root, String name, int uvX, int uvY, float minX, float minY, float minZ, float sizeX, float sizeY, float sizeZ) {
		root.addOrReplaceChild(
				name,
				CubeListBuilder.create().texOffs(uvX, uvY).addBox(minX, minY, minZ, sizeX, sizeY, sizeZ),
				PartPose.offsetAndRotation(sizeX + minX * 2, sizeY + minY * 2, 0, 0, 0, (float) Math.PI));
	}

	static {
		graveModel = getGraveModel();
	}

	private record TextRenderInfo(float depth, float height, float width) { }
	private record SkullRenderInfo(float depth, float height, int[] rotation, float scaleFace, float scaleDepth) { }

	public static class GraveRenderState extends BlockEntityRenderState {
		public MonkeySMPGraveyardConfig.GraveRendering config;
		public Direction direction;
		@Nullable
		public ResolvableProfile skullOwner;
		@Nullable
		public Component graveText;
		public boolean glowing;
	}
}
