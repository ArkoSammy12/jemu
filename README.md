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

## Building

A Java Development Kit targeting Java version 25 or later is required to build this project.

Clone the repository and run the following command on the top level directory:

```
mvnw clean package
```

An execute `.jar` file should have then been generated in `/target/jemu-x.y.z.jar`.

## License

This project is licensed under the [MIT License](LICENSE).
