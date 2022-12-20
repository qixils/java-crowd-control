# java-crowd-control [![Build Status](https://img.shields.io/github/actions/workflow/status/qixils/java-crowd-control/maven.yml?event=push&branch=main)](https://github.com/qixils/java-crowd-control/actions/workflows/maven.yml) [![Release](https://img.shields.io/maven-central/v/dev.qixils.crowdcontrol/java-crowd-control?color=success)](https://search.maven.org/artifact/dev.qixils.crowdcontrol/java-crowd-control)

A set of Java libraries for interacting with or simulating a
[crowdcontrol.live](https://crowdcontrol.live) service. The project abstracts away the complex logic
involved in communicating with connected services, allowing you to quickly and easily develop new
effect packs without having to worry about complicated networking protocols. Various helpful
utilities are included, such as a manager for timed effects which prevents two of the same effect
from running at the same time.

The two main libraries included in this project are:

- **Receiver**: Allows video games to easily accept incoming effect purchases from a
  crowdcontrol.live service (typically a streamer running the Crowd Control desktop application)
  and apply them to the connected user(s).
- **Sender**: Allows software to simulate a crowdcontrol.live service by sending arbitrary effect
  purchases to the connected video game(s).

## Usage

Detailed instructions for how to use these Java libraries are available
[**here**](https://github.com/qixils/java-crowd-control/blob/master/USAGE.md).

## License

This project is licensed under the MIT license. The license is available to view
[here](https://github.com/qixils/java-crowd-control/blob/master/LICENSE).
