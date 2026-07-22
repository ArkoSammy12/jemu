# Changelog

## [Unreleased]

- Add Atari 2600 core, with support for left and right joysticks, and a [cartridge database](https://github.com/munsie/vcs_cart_db) for game metadata.
- Add a new CHIP-8 system category, containing the newly added cores:
  - CHIP-8
  - STRICT CHIP-8
  - CHIP-8X
  - CHIP-48
  - SUPER-CHIP 1.0
  - SUPER-CHIP 1.1
  - SUPER-CHIP MODERN
  - XO-CHIP
  - MEGA-CHIP
  - HyperWaveCHIP-64
- The VIP CHIP-8 and VIP CHIP-8X system options have been moved to the new CHIP-8 category.
- Remove `bin` file extension for the COSMAC-VIP and RCA Studio II cores.
- Add support for the `nes20db` database file for retrieving NES game metadata.
- Add per-system settings:
  - Added Game Boy palette settings when playing with the DMG.
  - Added Atari 2600 settings for controlling the console switches, and overriding the TV format and cartridge type.

## 1.0.0

- Initial release