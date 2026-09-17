package com.stereowalker.tiered.gson;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class EntityAttributeModifierSerializer implements JsonSerializer<AttributeModifier> {

	@Override
	public JsonElement serialize(AttributeModifier src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject obj = new JsonObject();
		obj.addProperty("amount", src.amount());
		obj.addProperty("operation", src.operation().toString());
		obj.addProperty("id", src.id().toString());
		return obj;
	}
}