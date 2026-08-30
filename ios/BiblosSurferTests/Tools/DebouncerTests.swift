//
//  DebouncerTests.swift
//  BiblosSurferTests
//

import XCTest
@testable import BiblosSurfer

final class DebouncerTests: XCTestCase {
    func testFlushRunsPendingActionImmediately() {
        let debouncer = Debouncer()
        var ran = false
        debouncer.schedule(after: 10) {
            ran = true
        }
        debouncer.flush()
        XCTAssertTrue(ran)
    }

    func testCancelDropsPendingAction() {
        let debouncer = Debouncer()
        var ran = false
        debouncer.schedule(after: 10) {
            ran = true
        }
        debouncer.cancel()
        debouncer.flush()
        XCTAssertFalse(ran)
    }
}
