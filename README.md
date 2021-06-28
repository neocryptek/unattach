# Unattach

See [homepage](https://unattach.app/) for more info.

## Legal
* [LICENSE](LICENSE).
  * tl;dr: No sharing. For personal use only. Can contribute via PRs to this repo.
* [PRIVACY](PRIVACY).
  * tl;dr: Your personal data flows only between Google servers and your computer.

## Dependencies
* Install [Java 16](https://www.oracle.com/java/technologies/javase-downloads.html).
* Install [Maven](https://maven.apache.org/download.cgi).

## Build & Run
* Build with `mvn clean package`.
* Run with `java -jar target/client-3.2.0-jar-with-dependencies.jar`.

## Known Limitations
* On some emails, the app will fail with `OutOfMemoryError` even with the maximum heap size set to 2GB. This occurs
  when the Gmail API client library unpacks the downloaded email in local memory using a third-party JSON library, which
  appears to sometimes make inefficient use of the available memory. If this happens, the original email will remain
  intact, the memory will be recovered, and the processing will continue with the next email.

## Contributions
Feel free to
[report issues](https://help.github.com/en/articles/creating-an-issue) and
[create pull requests](https://help.github.com/en/articles/creating-a-pull-request).

## Support Unattach
If you like Unattach:
* [Buy Developers a Coffee](https://unattach.app/#support) ☕, or
* [Become a Sponsor](https://github.com/sponsors/rokstrnisa) ❤️

## Current Sponsors
### Bronze
[![Smart Cities Transport](src/main/resources/smart-cities-transport-logo.png)](https://smartcitiestransport.com/)
