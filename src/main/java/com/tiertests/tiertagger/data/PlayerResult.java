package com.tiertests.tiertagger.data;

import java.util.UUID;

public record PlayerResult(String name, UUID uuid, PlayerData data) {
}