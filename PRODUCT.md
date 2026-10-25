# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Kotlin + Jetpack Compose, CameraX for capture, local storage on the device. No server.

## Users

One person: the owner, journaling their own daily life on their own Android phone. They capture moments during the day (photos, quick lines of text) and look back later.

## Product Purpose

A private daily journal that is as fast as Snapchat to capture into. Success: the owner keeps logging past week 2, and scrolling back through days and months is something they enjoy.

## Positioning

Capture-first, not writing-first: the app opens to the camera, and the journal is organised as days, not as a feed or a document. Each logged day becomes a tile; busier days are bigger.

## Operating Context

- Captures happen in passing: one-handed, often outdoors, in seconds.
- Writing happens in quieter moments (evening), prompted by one daily reminder.
- Photos also arrive from the phone's gallery, taken with the normal camera; they keep their original date.

## Capabilities and Constraints

- Entry types: photo, photo with optional caption, text-only. One photo per entry.
- Gallery import: multi-select; each photo becomes its own entry dated by its original timestamp; photos without a date default to today with a visible note.
- Edit and delete any entry (caption, text, date).
- Fingerprint/PIN lock, on by default.
- One daily reminder at a user-chosen time.
- Version 1 browsing: month view of day tiles, day view, entry view.
- Version 2: automatic backup to the owner's own Cloudflare R2, with tested restore. Week 3: "On this day".
- Out of scope: mood, tags, search, location, video, sharing, multiple devices.
- Undecided: app name.

## Evidence on Hand

No real photos or entries yet. Mockups use placeholder tones and invented sample entries; nothing is to be presented as real user content.

## Product Principles

1. Capture in one action from opening the app.
2. Private by default: nothing lands in the gallery or leaves the phone without the owner choosing it.
3. Days are the unit of memory.
4. Ship the smallest version that gets daily use; add only what use proves is missing.
