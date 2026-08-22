//
//  LaunchUITests.swift
//  BiblosSurferUITests
//

import XCTest

final class LaunchUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["-UITestStub"]
        app.launch()
    }

    func testAppLaunches() {
        XCTAssertEqual(app.state, .runningForeground)
        attachScreenshot(of: app, named: "launch")
        attachSuccessReport(testName: "testAppLaunches")
    }

    func testOpeningSampleBookShowsItsText() {
        let cell = app.buttons["library.cell.The Sample Voyage"]
        XCTAssertTrue(cell.waitForExistence(timeout: 8), "Sample book tile should appear on the library")
        attachScreenshot(of: app, named: "library")
        cell.tap()

        let container = app.descendants(matching: .any)["reader.container"]
        XCTAssertTrue(container.waitForExistence(timeout: 20), "Reader should appear after tapping the tile")
        attachScreenshot(of: app, named: "book")
        attachSuccessReport(testName: "testOpeningSampleBookShowsItsText")
    }
}
