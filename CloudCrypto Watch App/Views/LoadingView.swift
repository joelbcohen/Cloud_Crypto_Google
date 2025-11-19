//
//  LoadingView.swift
//  CloudCrypto Watch App
//
//  Created by Claude AI
//  Copyright © 2025 Callista. All rights reserved.
//

import SwiftUI

struct LoadingView: View {
    var body: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)

            Text("Processing...")
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }
}

#Preview {
    LoadingView()
}
