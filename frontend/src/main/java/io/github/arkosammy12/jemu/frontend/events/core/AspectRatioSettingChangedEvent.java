package io.github.arkosammy12.jemu.frontend.events.core;

import io.github.arkosammy12.jemu.frontend.config.settings.VideoSettings;

public record AspectRatioSettingChangedEvent(VideoSettings.AspectRatio aspectRatio) implements VideoSettingChangedEvent {}
