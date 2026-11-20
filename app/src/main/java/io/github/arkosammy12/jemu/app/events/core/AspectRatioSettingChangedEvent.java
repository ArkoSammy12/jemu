package io.github.arkosammy12.jemu.app.events.core;

import io.github.arkosammy12.jemu.app.config.settings.VideoSettings;
import io.github.arkosammy12.jemu.app.events.ListenableEvent;
import io.github.arkosammy12.jemu.app.events.VideoSettingChangedEvent;

public record AspectRatioSettingChangedEvent(VideoSettings.AspectRatio aspectRatio) implements VideoSettingChangedEvent, ListenableEvent {}
