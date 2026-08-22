//
//  LocatorRoundTripTests.swift
//  BiblosSurferTests
//

import ReadiumShared
import XCTest
@testable import BiblosSurfer

/// The reading position is stored as `Locator` JSON, so a broken round trip means every reader
/// silently reopens at page one. These tests also pin the wire format, which has to stay readable by
/// the Kotlin toolkit if the Android port ever lands.
final class LocatorRoundTripTests: XCTestCase {
    func testLocatorSurvivesJSONRoundTrip() throws {
        let original = TestFixtures.locator()

        let json = try original.jsonString()
        let restored = try Locator(jsonString: json)

        XCTAssertEqual(restored.href, original.href)
        XCTAssertEqual(restored.mediaType, original.mediaType)
        XCTAssertEqual(restored.title, original.title)
        XCTAssertEqual(restored.locations.progression, original.locations.progression)
        XCTAssertEqual(restored.locations.totalProgression, original.locations.totalProgression)
        XCTAssertEqual(restored.locations.position, original.locations.position)
        XCTAssertEqual(restored.text.highlight, original.text.highlight)
    }

    func testJSONUsesTheKeysSharedWithTheKotlinToolkit() throws {
        let json = try TestFixtures.locator().jsonString()
        let object = try XCTUnwrap(
            JSONSerialization.jsonObject(with: XCTUnwrap(json.data(using: .utf8))) as? [String: Any]
        )

        XCTAssertNotNil(object["href"])
        XCTAssertEqual(object["type"] as? String, MediaType.xhtml.string)
        XCTAssertNotNil(object["locations"])
        XCTAssertNotNil(object["text"])
    }

    func testMinimalLocatorRoundTripsWithoutOptionalFields() throws {
        let original = TestFixtures.locator(
            title: nil,
            progression: nil,
            totalProgression: nil,
            position: nil,
            highlight: nil
        )

        let restored = try Locator(jsonString: try original.jsonString())

        XCTAssertEqual(restored.href, original.href)
        XCTAssertNil(restored.title)
        XCTAssertNil(restored.locations.progression)
        XCTAssertNil(restored.locations.position)
    }
}
