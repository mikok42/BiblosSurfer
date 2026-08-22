//
//  XCTestCase+Attachments.swift
//  BiblosSurferUITests
//

import XCTest

extension XCTestCase {
    func attachScreenshot(of app: XCUIApplication, named name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    func attachSuccessReport(testName: String) {
        let body = """
        status: SUCCESS
        test: \(testName)
        timestamp: \(ISO8601DateFormatter().string(from: Date()))
        """
        let attachment = XCTAttachment(string: body)
        attachment.name = "\(testName)-report"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
