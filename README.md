# jemu

Multi-system emulator written in Java.

![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-25-blue)

## Currently supported cores

- [x] COSMAC-VIP: Based on the CDP1802 CPU core.
- [x] VIP CHIP-8: COSMAC-VIP core running the CHIP-8 interpreter.
- [x] VIP CHIP-8X: COSMAC-VIP core with the VP-590 color expansion board and the VP-595 sound expansion board running the CHIP-8X interpreter.
- [x] Game Boy: DMG model based on the SM83 CPU core.
- [x] Game Boy Color: CGB model based on the SM83 CPU core.
- [ ] Nintendo Entertainment System (WIP).
- [ ] Commodore 64 (planned).
- [ ] Atari 2600 (planned).
- [ ] Sega Master System (planned).
- [ ] ZX Spectrum (planned).
- [ ] Sega Genesis (planned).

## Command-line Usage

If you launch **jemu** from the CLI, you can optionally pass arguments.

Usage:

```
jemu [-hV] -r=<romPath> [-s=<system>]
```

| Argument                                                                   | Description                                                                      | Default |
|----------------------------------------------------------------------------|----------------------------------------------------------------------------------|---------|
| `-r, --rom <path>`                                                         | **Required.** Path to the ROM file (absolute or relative to the JAR).            | -       |
| `-s, --system <cosmac-vip\|vip-chip8\|vip-chip8x\|gameboy\|gameboy-color>` | Launch with desired system selected or leave unspecified to use current setting. | -       |
| `-h, --help`                                                               | Show the help message and exit.                                                  | -       |
| `V, --version`                                                             | Print version information and exit.                                              | -       |



## Building

A Java Development Kit targeting Java version 25 or later is required to build this project.

Clone the repository and run the following command on the top level directory:

```
mvnw clean package
```

An executable `.jar` file should have then been generated in `/target/jemu-x.y.z.jar`.

Run with the `-DskipTests` flag to omit running the automated unit tests.

## License

This project is licensed under the [MIT License](LICENSE).
