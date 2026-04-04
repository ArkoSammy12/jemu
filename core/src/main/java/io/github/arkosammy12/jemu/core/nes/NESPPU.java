package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.common.VideoGenerator;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;

import java.util.Arrays;
import java.util.LinkedList;

import static io.github.arkosammy12.jemu.core.nes.NESCPUMMIOBus.*;

public class NESPPU<E extends NESEmulator> extends VideoGenerator<E> implements Bus {

    public static final int[] PALETTE_2C02G_WIKI_PAL = {
                0x62, 0x62, 0x62, 0x00, 0x1c, 0x95, 0x19, 0x04, 0xac, 0x42, 0x00, 0x9d,
                0x61, 0x00, 0x6b, 0x6e, 0x00, 0x25, 0x65, 0x05, 0x00, 0x49, 0x1e, 0x00,
                0x22, 0x37, 0x00, 0x00, 0x49, 0x00, 0x00, 0x4f, 0x00, 0x00, 0x48, 0x16,
                0x00, 0x35, 0x5e, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xab, 0xab, 0xab, 0x0c, 0x4e, 0xdb, 0x3d, 0x2e, 0xff, 0x71, 0x15, 0xf3,
                0x9b, 0x0b, 0xb9, 0xb0, 0x12, 0x62, 0xa9, 0x27, 0x04, 0x89, 0x46, 0x00,
                0x57, 0x66, 0x00, 0x23, 0x7f, 0x00, 0x00, 0x89, 0x00, 0x00, 0x83, 0x32,
                0x00, 0x6d, 0x90, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xff, 0xff, 0xff, 0x57, 0xa5, 0xff, 0x82, 0x87, 0xff, 0xb4, 0x6d, 0xff,
                0xdf, 0x60, 0xff, 0xf8, 0x63, 0xc6, 0xf8, 0x74, 0x6d, 0xde, 0x90, 0x20,
                0xb3, 0xae, 0x00, 0x81, 0xc8, 0x00, 0x56, 0xd5, 0x22, 0x3d, 0xd3, 0x6f,
                0x3e, 0xc1, 0xc8, 0x4e, 0x4e, 0x4e, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xff, 0xff, 0xff, 0xbe, 0xe0, 0xff, 0xcd, 0xd4, 0xff, 0xe0, 0xca, 0xff,
                0xf1, 0xc4, 0xff, 0xfc, 0xc4, 0xef, 0xfd, 0xca, 0xce, 0xf5, 0xd4, 0xaf,
                0xe6, 0xdf, 0x9c, 0xd3, 0xe9, 0x9a, 0xc2, 0xef, 0xa8, 0xb7, 0xef, 0xc4,
                0xb6, 0xea, 0xe5, 0xb8, 0xb8, 0xb8, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x64, 0x47, 0x40, 0x00, 0x09, 0x70, 0x1a, 0x00, 0x85, 0x41, 0x00, 0x78,
                0x5e, 0x00, 0x4d, 0x6c, 0x00, 0x0f, 0x64, 0x00, 0x00, 0x49, 0x13, 0x00,
                0x23, 0x28, 0x00, 0x00, 0x36, 0x00, 0x00, 0x39, 0x00, 0x00, 0x30, 0x00,
                0x00, 0x1e, 0x3f, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xac, 0x83, 0x81, 0x11, 0x31, 0xae, 0x40, 0x16, 0xcf, 0x72, 0x03, 0xc4,
                0x9a, 0x00, 0x91, 0xae, 0x04, 0x42, 0xa7, 0x19, 0x00, 0x88, 0x35, 0x00,
                0x58, 0x51, 0x00, 0x25, 0x65, 0x00, 0x00, 0x6b, 0x00, 0x00, 0x62, 0x18,
                0x00, 0x4d, 0x6b, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xfe, 0xce, 0xd2, 0x5d, 0x7e, 0xe8, 0x87, 0x63, 0xff, 0xb7, 0x4d, 0xff,
                0xe0, 0x43, 0xe3, 0xf7, 0x47, 0x9b, 0xf4, 0x58, 0x47, 0xdc, 0x73, 0x01,
                0xb1, 0x8e, 0x00, 0x81, 0xa4, 0x00, 0x58, 0xae, 0x07, 0x42, 0xaa, 0x4f,
                0x44, 0x99, 0xa2, 0x50, 0x35, 0x2d, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xfe, 0xce, 0xd2, 0xc0, 0xb1, 0xd7, 0xcf, 0xa7, 0xe9, 0xe1, 0x9d, 0xea,
                0xf1, 0x99, 0xdc, 0xfb, 0x99, 0xc1, 0xfb, 0x9e, 0xa0, 0xf3, 0xa9, 0x85,
                0xe4, 0xb3, 0x73, 0xd2, 0xbd, 0x72, 0xc2, 0xc1, 0x80, 0xb8, 0xc1, 0x9b,
                0xb8, 0xbc, 0xbb, 0xb9, 0x8f, 0x8e, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x3d, 0x5d, 0x36, 0x00, 0x19, 0x73, 0x02, 0x01, 0x84, 0x25, 0x00, 0x72,
                0x3f, 0x00, 0x43, 0x4a, 0x00, 0x05, 0x44, 0x04, 0x00, 0x2c, 0x1d, 0x00,
                0x0a, 0x35, 0x00, 0x00, 0x46, 0x00, 0x00, 0x4d, 0x00, 0x00, 0x45, 0x03,
                0x00, 0x32, 0x44, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x7a, 0xa1, 0x6a, 0x00, 0x47, 0xad, 0x1f, 0x28, 0xc9, 0x4d, 0x11, 0xb9,
                0x71, 0x08, 0x82, 0x83, 0x10, 0x33, 0x7d, 0x26, 0x00, 0x61, 0x44, 0x00,
                0x37, 0x63, 0x00, 0x08, 0x7b, 0x00, 0x00, 0x85, 0x00, 0x00, 0x7d, 0x17,
                0x00, 0x67, 0x6c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xc7, 0xf0, 0xaf, 0x32, 0x9b, 0xd9, 0x5a, 0x7d, 0xfe, 0x88, 0x65, 0xf9,
                0xaf, 0x5a, 0xcd, 0xc5, 0x5d, 0x84, 0xc4, 0x6f, 0x32, 0xac, 0x8b, 0x00,
                0x85, 0xa9, 0x00, 0x57, 0xc1, 0x00, 0x2f, 0xcc, 0x00, 0x1a, 0xc9, 0x41,
                0x1b, 0xb7, 0x94, 0x2b, 0x49, 0x26, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xc7, 0xf0, 0xaf, 0x8b, 0xd2, 0xba, 0x9a, 0xc7, 0xcb, 0xac, 0xbe, 0xcc,
                0xbc, 0xb9, 0xbe, 0xc5, 0xb9, 0xa3, 0xc6, 0xbf, 0x83, 0xbe, 0xc9, 0x67,
                0xaf, 0xd4, 0x56, 0x9e, 0xdd, 0x55, 0x8d, 0xe2, 0x63, 0x84, 0xe2, 0x7f,
                0x83, 0xdc, 0x9e, 0x86, 0xae, 0x75, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x44, 0x46, 0x2f, 0x00, 0x08, 0x66, 0x08, 0x00, 0x77, 0x29, 0x00, 0x68,
                0x42, 0x00, 0x3d, 0x4d, 0x00, 0x00, 0x46, 0x00, 0x00, 0x2f, 0x12, 0x00,
                0x0e, 0x26, 0x00, 0x00, 0x34, 0x00, 0x00, 0x38, 0x00, 0x00, 0x2f, 0x00,
                0x00, 0x1d, 0x3a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x81, 0x81, 0x65, 0x00, 0x30, 0xa0, 0x27, 0x16, 0xbb, 0x53, 0x02, 0xac,
                0x76, 0x00, 0x79, 0x87, 0x04, 0x2c, 0x80, 0x18, 0x00, 0x64, 0x34, 0x00,
                0x3a, 0x4f, 0x00, 0x0e, 0x63, 0x00, 0x00, 0x69, 0x00, 0x00, 0x61, 0x12,
                0x00, 0x4c, 0x62, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xcc, 0xca, 0xac, 0x3c, 0x7c, 0xd0, 0x64, 0x61, 0xf3, 0x90, 0x4b, 0xee,
                0xb6, 0x42, 0xc3, 0xca, 0x46, 0x7c, 0xc7, 0x57, 0x2e, 0xb0, 0x71, 0x00,
                0x89, 0x8d, 0x00, 0x5c, 0xa2, 0x00, 0x36, 0xab, 0x00, 0x22, 0xa7, 0x3f,
                0x25, 0x96, 0x8e, 0x32, 0x34, 0x1f, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xcc, 0xca, 0xac, 0x93, 0xae, 0xb6, 0xa1, 0xa3, 0xc6, 0xb3, 0x9a, 0xc7,
                0xc3, 0x96, 0xb8, 0xcb, 0x96, 0x9d, 0xcb, 0x9c, 0x7f, 0xc3, 0xa6, 0x64,
                0xb4, 0xb1, 0x54, 0xa3, 0xba, 0x53, 0x93, 0xbe, 0x63, 0x8b, 0xbd, 0x7d,
                0x8b, 0xb8, 0x9b, 0x8d, 0x8d, 0x71, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x4c, 0x49, 0x78, 0x00, 0x0f, 0x9e, 0x13, 0x00, 0xb3, 0x37, 0x00, 0xa3,
                0x51, 0x00, 0x72, 0x5a, 0x00, 0x2f, 0x4f, 0x00, 0x00, 0x34, 0x0c, 0x00,
                0x0f, 0x21, 0x00, 0x00, 0x31, 0x00, 0x00, 0x38, 0x00, 0x00, 0x33, 0x27,
                0x00, 0x24, 0x6a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x88, 0x89, 0xc3, 0x04, 0x3a, 0xe8, 0x32, 0x1e, 0xff, 0x62, 0x07, 0xfb,
                0x86, 0x00, 0xc1, 0x95, 0x01, 0x6d, 0x8b, 0x14, 0x15, 0x6a, 0x2f, 0x00,
                0x3d, 0x4b, 0x00, 0x0e, 0x61, 0x00, 0x00, 0x6b, 0x00, 0x00, 0x67, 0x4b,
                0x00, 0x55, 0xa2, 0x00, 0x00, 0x0d, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xd1, 0xd7, 0xff, 0x42, 0x87, 0xff, 0x6b, 0x6b, 0xff, 0x9a, 0x54, 0xff,
                0xc1, 0x47, 0xff, 0xd5, 0x49, 0xd4, 0xd1, 0x59, 0x7f, 0xb7, 0x72, 0x39,
                0x8f, 0x8f, 0x12, 0x60, 0xa6, 0x15, 0x39, 0xb2, 0x42, 0x25, 0xb0, 0x8d,
                0x28, 0xa1, 0xe2, 0x3a, 0x36, 0x62, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xd1, 0xd7, 0xff, 0x98, 0xba, 0xff, 0xa7, 0xaf, 0xff, 0xb9, 0xa6, 0xff,
                0xc9, 0xa0, 0xff, 0xd2, 0xa0, 0xff, 0xd2, 0xa6, 0xe3, 0xca, 0xaf, 0xc6,
                0xbb, 0xbb, 0xb6, 0xa9, 0xc4, 0xb4, 0x99, 0xc9, 0xc3, 0x90, 0xc9, 0xde,
                0x8f, 0xc4, 0xfe, 0x94, 0x95, 0xcf, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x4b, 0x3c, 0x4e, 0x00, 0x04, 0x77, 0x12, 0x00, 0x8b, 0x35, 0x00, 0x7d,
                0x4e, 0x00, 0x53, 0x57, 0x00, 0x18, 0x4e, 0x00, 0x00, 0x33, 0x09, 0x00,
                0x0f, 0x1d, 0x00, 0x00, 0x2b, 0x00, 0x00, 0x2f, 0x00, 0x00, 0x28, 0x0b,
                0x00, 0x19, 0x47, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x88, 0x75, 0x8f, 0x05, 0x2a, 0xb8, 0x32, 0x10, 0xd6, 0x60, 0x00, 0xca,
                0x83, 0x00, 0x97, 0x92, 0x00, 0x4b, 0x88, 0x0e, 0x00, 0x69, 0x29, 0x00,
                0x3c, 0x44, 0x00, 0x0f, 0x57, 0x00, 0x00, 0x5e, 0x00, 0x00, 0x58, 0x28,
                0x00, 0x45, 0x78, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xd2, 0xbd, 0xdc, 0x44, 0x73, 0xf5, 0x6c, 0x58, 0xff, 0x9a, 0x42, 0xff,
                0xbf, 0x38, 0xea, 0xd3, 0x3c, 0xa3, 0xcf, 0x4c, 0x52, 0xb6, 0x65, 0x10,
                0x8e, 0x80, 0x00, 0x60, 0x95, 0x00, 0x3b, 0x9f, 0x1b, 0x27, 0x9c, 0x62,
                0x2b, 0x8c, 0xb3, 0x39, 0x2b, 0x3c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xd2, 0xbd, 0xdc, 0x99, 0xa2, 0xe4, 0xa8, 0x98, 0xf3, 0xba, 0x8f, 0xf4,
                0xc9, 0x8a, 0xe5, 0xd2, 0x8a, 0xca, 0xd2, 0x90, 0xab, 0xca, 0x9a, 0x91,
                0xbb, 0xa4, 0x81, 0xa9, 0xad, 0x80, 0x9a, 0xb2, 0x8f, 0x91, 0xb1, 0xaa,
                0x91, 0xac, 0xc9, 0x94, 0x81, 0x9b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x39, 0x46, 0x4a, 0x00, 0x0d, 0x7a, 0x03, 0x00, 0x8a, 0x25, 0x00, 0x78,
                0x3e, 0x00, 0x4a, 0x47, 0x00, 0x0f, 0x3f, 0x00, 0x00, 0x27, 0x0d, 0x00,
                0x06, 0x21, 0x00, 0x00, 0x31, 0x00, 0x00, 0x37, 0x00, 0x00, 0x31, 0x12,
                0x00, 0x22, 0x4e, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x70, 0x83, 0x86, 0x00, 0x36, 0xb9, 0x1f, 0x1a, 0xd3, 0x4c, 0x04, 0xc3,
                0x6e, 0x00, 0x8c, 0x7d, 0x02, 0x40, 0x75, 0x15, 0x00, 0x59, 0x2f, 0x00,
                0x2f, 0x4b, 0x00, 0x02, 0x60, 0x00, 0x00, 0x69, 0x00, 0x00, 0x64, 0x2d,
                0x00, 0x51, 0x7c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xb7, 0xce, 0xcd, 0x2f, 0x80, 0xef, 0x57, 0x64, 0xff, 0x83, 0x4e, 0xff,
                0xa7, 0x44, 0xdf, 0xbb, 0x46, 0x97, 0xb8, 0x57, 0x48, 0xa0, 0x70, 0x07,
                0x78, 0x8c, 0x00, 0x4c, 0xa2, 0x00, 0x28, 0xad, 0x16, 0x14, 0xaa, 0x5e,
                0x17, 0x9a, 0xad, 0x28, 0x34, 0x39, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xb7, 0xce, 0xcd, 0x80, 0xb1, 0xd8, 0x8f, 0xa6, 0xe7, 0xa0, 0x9e, 0xe7,
                0xaf, 0x99, 0xd9, 0xb8, 0x99, 0xbe, 0xb9, 0x9f, 0x9f, 0xb0, 0xa9, 0x84,
                0xa1, 0xb4, 0x74, 0x90, 0xbc, 0x74, 0x81, 0xc1, 0x82, 0x78, 0xc1, 0x9d,
                0x78, 0xbb, 0xbd, 0x7c, 0x8f, 0x91, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x3d, 0x3d, 0x3d, 0x00, 0x05, 0x6c, 0x06, 0x00, 0x7c, 0x27, 0x00, 0x6d,
                0x40, 0x00, 0x43, 0x49, 0x00, 0x08, 0x40, 0x00, 0x00, 0x29, 0x0a, 0x00,
                0x08, 0x1d, 0x00, 0x00, 0x2b, 0x00, 0x00, 0x2f, 0x00, 0x00, 0x29, 0x06,
                0x00, 0x19, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x76, 0x76, 0x76, 0x00, 0x2b, 0xa8, 0x24, 0x10, 0xc2, 0x4f, 0x00, 0xb4,
                0x71, 0x00, 0x81, 0x80, 0x00, 0x36, 0x78, 0x10, 0x00, 0x5b, 0x2a, 0x00,
                0x31, 0x45, 0x00, 0x06, 0x58, 0x00, 0x00, 0x5f, 0x00, 0x00, 0x58, 0x1f,
                0x00, 0x45, 0x6c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xbd, 0xbd, 0xbd, 0x35, 0x72, 0xde, 0x5d, 0x58, 0xff, 0x88, 0x43, 0xfa,
                0xac, 0x39, 0xcf, 0xbf, 0x3d, 0x89, 0xbc, 0x4d, 0x3b, 0xa3, 0x66, 0x00,
                0x7c, 0x81, 0x00, 0x51, 0x96, 0x00, 0x2d, 0xa0, 0x0a, 0x1a, 0x9c, 0x50,
                0x1d, 0x8b, 0x9e, 0x2c, 0x2c, 0x2c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0xbd, 0xbd, 0xbd, 0x87, 0xa2, 0xc7, 0x95, 0x97, 0xd7, 0xa6, 0x8f, 0xd7,
                0xb5, 0x8a, 0xc8, 0xbe, 0x8b, 0xad, 0xbe, 0x91, 0x8f, 0xb5, 0x9a, 0x75,
                0xa7, 0xa4, 0x65, 0x95, 0xad, 0x65, 0x87, 0xb2, 0x74, 0x7e, 0xb1, 0x8e,
                0x7e, 0xab, 0xad, 0x81, 0x81, 0x81, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };


    public static final int CHR_ROM_START = 0x0000;
    public static final int CHR_ROM_END = 0x1FFF;

    public static final int CIRAM_START = 0x2000;
    public static final int CIRAM_END = 0x2FFF;

    public static final int CIRAM_MIRROR_START = 0x3000;
    public static final int CIRAM_MIRROR_END = 0x3EFF;

    public static final int PALETTE_RAM_START = 0x3F00;
    public static final int PALETTE_RAM_END = 0x3F1F;

    public static final int PALETTE_RAM_MIRROR_START = 0x3F20;
    public static final int PALETTE_RAM_MIRROR_END = 0x3FFF;

    private static final int WIDTH = 256;

    private static final int DOTS_PER_SCANLINE = 341;
    private static final int FIRST_VISIBLE_DOT = 1;
    private static final int LAST_VISIBLE_DOT = 256;

    private static final int NTSC_SCANLINES_PER_FRAME = 262;
    private static final int NTSC_VISIBLE_SCANLINES = 240;

    private static final int PAL_SCANLINES_PER_FRAME = 312;
    private static final int PAL_VISIBLE_SCANLINES = 239;

    private final int[][] video;
    private final int scanlinesPerFrame;
    private final int visibleScanlines;
    private final boolean oddFrameDotSkip;

    private final int[] oam = new int[256];
    private final int[] paletteRam = new int[0x20];

    private int ppuControl;
    private int ppuMask;
    private int ppuStatus;

    private int currentVRAMAddress; // v
    private int temporaryVRAMAddress; // t
    private int fineXScroll; // x
    private boolean writeLatch; // w

    private int currentOamAddress;

    private DotHalf currentDotHalf = DotHalf.FIRST;
    private int dotNumber;
    private int scanlineNumber;
    private boolean nmiSignal;
    private boolean ppuInit = true;

    private boolean oddFrame;
    private int ppuDataReadBuffer;
    private int copyTtoVCountdown;

    private final LinkedList<Integer> backgroundShiftRegister = new LinkedList<>();
    private final LinkedList<Integer> attributeShiftRegister = new LinkedList<>();

    private int attributeRegisterLatch = 0b00;

    private int bgFetcherStep = 0;
    private int bgFetcherTileNumber;
    private int bgFetcherAttributeByte;
    private int bgFetcherPatternTableLow;

    public NESPPU(E emulator) {
        super(emulator);
        this.scanlinesPerFrame = NTSC_SCANLINES_PER_FRAME;
        this.visibleScanlines = NTSC_VISIBLE_SCANLINES;
        this.oddFrameDotSkip = true;
        this.video = new int[WIDTH][this.visibleScanlines];
        for (int[] ints : this.video) {
            Arrays.fill(ints, 0xFF000000);
        }
        for (int i = 0; i < 16; i++) {
            this.backgroundShiftRegister.offer(0b00);
        }
        for (int i = 0; i < 8; i++) {
            this.attributeShiftRegister.offer(0b00);
        }
    }

    @Override
    public int getImageWidth() {
        return WIDTH;
    }

    @Override
    public int getImageHeight() {
        return this.visibleScanlines;
    }

    @Override
    public int readByte(int address) {
        return switch (address) {
            case PPUCTRL_ADDR, PPUMASK_ADDR, OAMADDR_ADDR, PPUADDR_ADDR, PPUSCROLL_ADDR -> 0xFF;
            case PPUSTATUS_ADDR -> {
                int value = this.ppuStatus;
                this.setVBlankFlag(false);
                this.clearW();
                yield value | 0b11111;
            }
            case OAMDATA_ADDR -> this.oam[this.currentOamAddress];
            case PPUDATA_ADDR -> {
                int readAddress = this.getV() & 0x3FFF;

                int ret = this.ppuDataReadBuffer;

                if (readAddress <= CHR_ROM_END) {
                    this.ppuDataReadBuffer = this.emulator.getCartridge().readBytePPU(readAddress);
                } else if (readAddress <= CIRAM_END) {
                    this.ppuDataReadBuffer = this.emulator.getCartridge().readBytePPU(readAddress);
                } else if (readAddress <= CIRAM_MIRROR_END) {
                    this.ppuDataReadBuffer = this.emulator.getCartridge().readBytePPU(readAddress);
                } else {
                    this.ppuDataReadBuffer = emulator.getCartridge().readBytePPU(readAddress);
                    int paletteAddr = readAddress & 0x1F;
                    if ((paletteAddr & 0x13) == 0x10) {
                        paletteAddr &= ~0x10;
                    }
                    ret = this.paletteRam[paletteAddr];
                }

                // TODO: If the $2007 access happens to coincide with a standard VRAM address increment (either horizontal or vertical), it will presumably not double-increment the relevant counter.
                if (this.isRenderingEnabled() && (this.isVisibleScanline() || this.isPreRenderScanline())) {
                    this.incrementHorizontalPosition();
                    this.incrementVerticalPosition();
                } else {
                    this.incrementV();
                }
                yield ret;
            }
            default -> throw new EmulatorException("Invalid address $%04X for NES PPU!".formatted(address));
        };
    }

    // TODO: Bus conflicts for VRAM and OAM during rendering

    @Override
    public void writeByte(int address, int value) {
        // Block register writes during the first frame until the vbl, sprite 0 and sprite overflow flags are cleared
        if (this.ppuInit) {
            return;
        }
        switch (address) {
            case PPUCTRL_ADDR -> {
                this.ppuControl = value & 0xFC;
                setT((getT() &  ~0xC00) | ((value & 0b11) << 10));
                this.setNMISignal(this.getVBlankNMIEnable());
            }
            case PPUMASK_ADDR -> this.ppuMask = value & 0xFF;
            case PPUSTATUS_ADDR -> {}
            case OAMADDR_ADDR -> this.currentOamAddress = value & 0xFF;
            case OAMDATA_ADDR -> {
                // TODO: Bus conflicts. Read OAMDATA register section in nesdev for more info.
                this.oam[this.currentOamAddress] = value & 0xFF;
                this.currentOamAddress = (this.currentOamAddress + 1) & 0xFF;
            }
            case PPUSCROLL_ADDR -> {
                if (this.getW()) {
                    this.setT((this.getT() & ~0x73E0) | ((value & 0b00000111) << 12) | ((value & 0b00111000) << 2) | ((value & 0b11000000) << 2));
                } else {
                    this.setT((this.getT() & ~0xF) | ((value >>> 3) & 0xF));
                    this.setX(value & 0b111);
                }
                this.toggleW();
            }
            case PPUADDR_ADDR -> {
                if (this.getW()) {
                    this.setT((this.getT() & ~0xFF) | (value & 0xFF));
                    // (wait 1 to 1.5 dots after the write completes as per nesdev)
                    this.copyTtoVCountdown = 3;
                } else {
                    this.setT((this.getT() & ~0x7F00) | ((value & 0b00111111) << 8));
                }
                this.toggleW();
            }
            case PPUDATA_ADDR -> {
                this.writeBytePPU(this.getV(), value);

                // TODO: If the $2007 access happens to coincide with a standard VRAM address increment (either horizontal or vertical), it will presumably not double-increment the relevant counter.
                if (this.isRenderingEnabled() && (this.isVisibleScanline() || this.isPreRenderScanline())) {
                    this.incrementHorizontalPosition();
                    this.incrementVerticalPosition();
                } else {
                    this.incrementV();
                }
            }
            default -> throw new EmulatorException("Invalid address $%04X for NES PPU!".formatted(address));
        };
    }

    private void setNMISignal(boolean value) {
        this.nmiSignal = value;
    }

    public boolean getNMISignal() {
        return this.nmiSignal && this.getVBlankFlag();
    }

    // TODO: Toggling rendering takes effect approximately 3-4 dots after the write. This delay is required by Battletoads to avoid a crash.
    private boolean isRenderingEnabled() {
        return this.enableBackgroundRendering() || this.enableSpriteRendering();
    }

    private boolean isPreRenderScanline() {
        return this.scanlineNumber == this.scanlinesPerFrame - 1;
    }

    private boolean isVisibleScanline() {
        return this.scanlineNumber < this.visibleScanlines;
    }

    private boolean isVisibleDot() {
        return this.dotNumber >= FIRST_VISIBLE_DOT && this.dotNumber <= LAST_VISIBLE_DOT;
    }

    private void incrementV() {
        setV(getV() + switch (this.getVramAddressIncrement()) {
            case VRAMAddressIncrement.ADD_1_ACROSS -> 1;
            case ADD_32_DOWN -> 32;
        });
    }

    // Dot 256 of each scanline if rendering is enabled
    private void incrementVerticalPosition() {
        if ((this.getV() & 0x7000) != 0x7000) {
            this.setV(this.getV() + 0x1000);
        } else {
            this.setV(this.getV() & ~0x7000);
            int y = (this.getV() & 0x03E0) >>> 5;
            if (y == 29) {
                y = 0;
                this.setV(this.getV() ^ 0x0800);
            } else if (y == 31) {
                y = 0;
            } else {
                y++;
            }
            this.setV((this.getV() & ~0x03E0) | ((y & 0x1F) << 5));
        }
    }

    // Between dot 328 of a scanline, and 256 of the next scanline
    private void incrementHorizontalPosition() {
        if ((this.getV() & 0x001F) == 31) {
            this.setV(this.getV() & ~0x001F);
            this.setV(this.getV() ^ 0x0400);
        } else {
            this.setV(getV() + 1);
        }
    }

    // Dot 257 of each scanline if rendering is enabled
    private void copyHorizontalPositionBitsToV() {
        this.setV((this.getV() & ~0x41F) | (this.getT() & 0x41F));
    }

    // During dots 280 to 304 of the pre-render scanline (end of vblank)
    private void copyVerticalPositionBitsToV() {
        this.setV((this.getV() & ~0x7BFF) | (this.getT() & 0x7BFF));
    }

    private void setV(int value) {
        this.currentVRAMAddress = value & 0x7FFF;
    }

    private int getV() {
        return this.currentVRAMAddress;
    }

    private void setT(int value) {
        this.temporaryVRAMAddress = value & 0x7FFF;
    }

    private int getT() {
        return this.temporaryVRAMAddress;
    }

    private void setX(int value) {
        this.fineXScroll = value & 0b111;
    }

    private int getX() {
        return this.fineXScroll;
    }

    private void clearW() {
        this.writeLatch = false;
    }

    private boolean getW() {
        return this.writeLatch;
    }

    private void toggleW() {
        this.writeLatch = !this.writeLatch;
    }

    public void cycleHalfDot() {

        if (this.copyTtoVCountdown > 0) {
            this.copyTtoVCountdown--;
            if (this.copyTtoVCountdown <= 0) {
                this.setV(this.getT());
            }
        }

        switch (this.currentDotHalf) {
            case FIRST -> {

            }
            case SECOND -> {

                if (this.isVisibleScanline()) {
                    if (this.isVisibleDot()) {
                        this.tickPixelShifter();
                        this.tickBgFetcher();
                    }

                    this.onRenderScanlineHBlank();

                } else if (this.scanlineNumber == this.visibleScanlines + 1) {
                    if (this.dotNumber == 1) {
                        this.emulator.getHost().getVideoDriver().ifPresent(driver -> driver.outputFrame(this.video));
                        this.setVBlankFlag(true);
                    }
                } else if (this.isPreRenderScanline()) {
                    if (this.isVisibleDot()) {
                        this.tickPixelShifter();
                        this.tickBgFetcher();
                    }
                    if (this.dotNumber == 1) {
                        this.setVBlankFlag(false);
                        this.setSprite0HitFlag(false);
                        this.setSpriteOverflowFlag(false);
                        if (this.ppuInit) {
                            this.ppuInit = false;
                        }
                    }

                    if (this.dotNumber >= 280 && this.dotNumber <= 304) {
                        if (this.isRenderingEnabled()) {
                            this.copyVerticalPositionBitsToV();
                        }
                    }

                    this.onRenderScanlineHBlank();
                }

                this.dotNumber++;
                if (this.dotNumber >= DOTS_PER_SCANLINE) {
                    this.dotNumber = 0;
                    this.scanlineNumber++;
                    if (this.scanlineNumber >= this.scanlinesPerFrame) {
                        this.scanlineNumber = 0;
                        this.oddFrame = !this.oddFrame;
                        if (this.oddFrameDotSkip && !this.oddFrame && this.isRenderingEnabled()) {
                            this.dotNumber = 1;
                        }
                    }
                }
            }
        }
        this.currentDotHalf = this.currentDotHalf.getOpposite();
    }

    private void tickPixelShifter() {
        int bgPixel = this.shiftBackgroundRegister(this.getX()) & 0b11;
        int bgPaletteNumber = this.shiftAttributeRegister(this.getX()) & 0b11;

        int paletteRamIndex;
        if (bgPixel == 0) {
            paletteRamIndex = 0;
        } else {
            paletteRamIndex = (bgPaletteNumber << 2) | bgPixel;
        }

        int paletteByte = this.paletteRam[paletteRamIndex];
        if (this.useGrayscaleColors()) {
            paletteByte &= 0x30;
        }

        int videoColorIndex = ((this.getEmphasisBits() << 6) | (paletteByte & 0b111111)) * 3;
        int red = PALETTE_2C02G_WIKI_PAL[videoColorIndex];
        int green = PALETTE_2C02G_WIKI_PAL[videoColorIndex + 1];
        int blue = PALETTE_2C02G_WIKI_PAL[videoColorIndex + 2];
        int argb = 0xFF000000 | (red << 16) | (green << 8) | blue;
        if (this.isVisibleDot() && this.isVisibleScanline()) {
            this.video[this.dotNumber - 1][this.scanlineNumber] = argb;
        }
    }

    private int shiftBackgroundRegister(int select) {
        this.backgroundShiftRegister.poll();
        this.backgroundShiftRegister.offer(0b10);
        return this.backgroundShiftRegister.get(select);
    }

    private int shiftAttributeRegister(int select) {
        this.attributeShiftRegister.poll();
        this.attributeShiftRegister.offer(this.attributeRegisterLatch);
        return this.attributeShiftRegister.get(select);
    }

    private void onRenderScanlineHBlank() {
        if (this.dotNumber == 256) {
            if (this.isRenderingEnabled()) {
                this.incrementVerticalPosition();
            }
        } else if (this.dotNumber == 257) {
            if (this.isRenderingEnabled()) {
                this.copyHorizontalPositionBitsToV();
            }
        } else if (this.dotNumber == 258) {
            this.readBytePPU(this.getNametableFetchAddress());
        } else if (this.dotNumber == 260) {
            this.readBytePPU(this.getNametableFetchAddress());
        } else if (this.dotNumber == 266) {
            this.readBytePPU(this.getNametableFetchAddress());
        } else if (this.dotNumber == 306) {
            this.readBytePPU(this.getNametableFetchAddress());
        } else if (this.dotNumber >= 321 && this.dotNumber <= 336) {
            this.tickPixelShifter();
            this.tickBgFetcher();
        } else if (this.dotNumber == 338) {
            this.readBytePPU(this.getNametableFetchAddress());
        } else if (dotNumber == 340) {
            this.readBytePPU(this.getNametableFetchAddress());
        }
    }

    private void tickBgFetcher() {
        switch (this.bgFetcherStep) {
            case 0 -> {
                this.bgFetcherStep = 1;
            }
            case 1 -> {
                this.bgFetcherTileNumber = this.readBytePPU(this.getNametableFetchAddress());
                this.bgFetcherStep = 2;
            }
            case 2 -> {
                this.bgFetcherStep = 3;
            }
            case 3 -> {
                int attributeAddress = 0x23C0 | (this.getV() & 0x0C00) | ((this.getV() >>> 4) & 0x38) | ((this.getV() >>> 2) & 0x07);
                this.bgFetcherAttributeByte = this.readBytePPU(attributeAddress);
                this.bgFetcherStep = 4;
            }
            case 4 -> {
                this.bgFetcherStep = 5;
            }
            case 5 -> {
                this.bgFetcherPatternTableLow = this.readBytePPU(this.getPatternByteAddress(false));
                this.bgFetcherStep = 6;
            }
            case 6 -> {
                this.bgFetcherStep = 7;
            }
            case 7 -> {
                int bgFetcherPatternTableHigh = this.readBytePPU(this.getPatternByteAddress(true));

                for (int i = 0; i < 8; i++) {
                    int bit = 7 - i;
                    int hi = (bgFetcherPatternTableHigh >>> bit) & 1;
                    int lo = (this.bgFetcherPatternTableLow >>> bit) & 1;
                    this.backgroundShiftRegister.set(i + 8, (hi << 1) | lo);
                }

                int coarseX = this.getV() & 0x1F;
                int coarseY = (this.getV() >>> 5) & 0x1F;
                int shift = ((coarseY & 0b10) | ((coarseX & 0b10) >>> 1)) * 2;
                this.attributeRegisterLatch = (this.bgFetcherAttributeByte >>> shift) & 0b11;

                if (this.isRenderingEnabled()) {
                    this.incrementHorizontalPosition();
                }
                this.bgFetcherStep = 0;
            }
        }
    }

    private int getNametableFetchAddress() {
        return 0x2000 | (this.getV() & 0x0FFF);
    }

    private int getPatternByteAddress(boolean highBitPlane) {
        return ((this.getV() >>> 12) & 0b111) | (highBitPlane ? (1 << 3) : 0) | ((this.bgFetcherTileNumber & 0xFF) << 4) | this.getBackgroundPatternTableAddress();
    }

    private VRAMAddressIncrement getVramAddressIncrement() {
        return (this.ppuControl & (1 << 2)) != 0 ? VRAMAddressIncrement.ADD_32_DOWN : VRAMAddressIncrement.ADD_1_ACROSS;
    }

    private int get8x8SpritePatternTableAddress() {
        return (this.ppuControl & (1 << 3)) != 0 ? 0x1000 : 0x0000;
    }

    private int getBackgroundPatternTableAddress() {
        return (this.ppuControl & (1 << 4)) != 0 ? 0x1000 : 0x0000;
    }

    private SpriteSize getSpriteSize() {
        return (this.ppuControl & (1 << 5)) != 0 ? SpriteSize.SIZE_8_16 : SpriteSize.SIZE_8_8;
    }

    private boolean getMasterSlaveSelect() {
        return (this.ppuControl & (1 << 6)) != 0;
    }

    private boolean getVBlankNMIEnable() {
        return (this.ppuControl & (1 << 7)) != 0;
    }

    private boolean useGrayscaleColors() {
        return (this.ppuMask & 1) != 0;
    }

    private boolean showBackgroundInLeftmost8Pixels() {
        return (this.ppuMask & (1 << 1)) != 0;
    }

    private boolean showSpritesInLeftmost8Pixels() {
        return (this.ppuMask & (1 << 2)) != 0;
    }

    private boolean enableBackgroundRendering() {
        return (this.ppuMask & (1 << 3)) != 0;
    }

    private boolean enableSpriteRendering() {
        return (this.ppuMask & (1 << 4)) != 0;
    }

    private int getEmphasisBits() {
        return ((this.ppuMask) >>> 5) & 0b111;
    }

    private void setSpriteOverflowFlag(boolean value) {
        if (value) {
            this.ppuStatus |= 1 << 5;
        } else {
            this.ppuStatus &= ~(1 << 5);
        }
    }

    private boolean getSpriteOverflowFlag() {
        return (this.ppuStatus & (1 << 5)) != 0;
    }

    private void setSprite0HitFlag(boolean value) {
        if (value) {
            this.ppuStatus |= 1 << 6;
        } else {
            this.ppuStatus &= ~(1 << 6);
        }
    }

    private boolean getSprite0Flag() {
        return (this.ppuStatus & (1 << 6)) != 0;
    }

    private void setVBlankFlag(boolean value) {
        if (value) {
            this.ppuStatus |= 1 << 7;
        } else {
            this.ppuStatus &= ~(1 << 7);
        }
    }

    private boolean getVBlankFlag() {
        return (this.ppuStatus & (1 << 7)) != 0;
    }

    private int readBytePPU(int address) {
        address &= 0x3FFF;
        if (address <= CHR_ROM_END) {
            return this.emulator.getCartridge().readBytePPU(address);
        } else if (address <= CIRAM_END) {
            return this.emulator.getCartridge().readBytePPU(address);
        } else if (address <= CIRAM_MIRROR_END) {
            return this.emulator.getCartridge().readBytePPU(address);
        } else {
            this.emulator.getCartridge().readBytePPU(address);
            int paletteAddr = address & 0x1F;
            if ((paletteAddr & 0x13) == 0x10) {
                paletteAddr &= ~0x10;
            }
            return this.paletteRam[paletteAddr];
        }
    }

    private void writeBytePPU(int address, int value) {
        address &= 0x3FFF;
        if (address <= CHR_ROM_END) {
            this.emulator.getCartridge().writeBytePPU(address, value);
        } else if (address <= CIRAM_END) {
            this.emulator.getCartridge().writeBytePPU(address, value);
        } else if (address <= CIRAM_MIRROR_END) {
            this.emulator.getCartridge().writeBytePPU(address, value);
        } else {
            this.emulator.getCartridge().writeBytePPU(address, value);
            int paletteAddr = address & 0x1F;
            if ((paletteAddr & 0x13) == 0x10) {
                paletteAddr &= ~0x10;
            }
            this.paletteRam[paletteAddr] = value & 0xFF;
        }
    }

    private enum VRAMAddressIncrement {
        ADD_1_ACROSS,
        ADD_32_DOWN
    }

    private enum SpriteSize {
        SIZE_8_8,
        SIZE_8_16
    }

    private enum DotHalf {
        FIRST,
        SECOND;

        private DotHalf getOpposite() {
            return switch (this) {
                case FIRST -> SECOND;
                case SECOND -> FIRST;
            };
        }

    }

}
