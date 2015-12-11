GenerateAIDL
==========

Introduction
------------
Extract and reconstruct AIDL files from DEX files.

Building
--------
Building requires Maven. To build:

    user@system$ mvn package

Output will be at `target/GenerateAIDL-*.jar`.

Usage
-----
Parse and extract AIDL files from DEX:

```
analyst$ java -jar GenerateAIDL-*.jar -i com.example.apk
Writing: IXTSrv.aidl
Writing: IXTSrv2.aidl
Writing: ITestService.aidl

```
