//
//  ProcessInfo+UITestStub.swift
//  BiblosSurfer
//
//  Created by Mikołaj Linczewski on 21/08/2026.
//

import Foundation

enum UITestLaunchArgument {
    static let stub = "-UITestStub"
}

extension ProcessInfo {
    var isUITestStubLaunch: Bool {
        arguments.contains(UITestLaunchArgument.stub)
    }
}
