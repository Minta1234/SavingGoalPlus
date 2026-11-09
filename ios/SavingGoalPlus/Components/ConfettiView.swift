import SwiftUI

public struct ConfettiView: View {
    @Binding public var trigger: Bool

    @State private var particles: [ConfettiParticle] = []
    @State private var timer: Timer?

    public init(trigger: Binding<Bool>) {
        self._trigger = trigger
    }

    public var body: some View {
        GeometryReader { proxy in
            ZStack {
                ForEach(particles) { particle in
                    RoundedRectangle(cornerRadius: 2)
                        .fill(particle.color)
                        .frame(width: particle.size.width, height: particle.size.height)
                        .rotationEffect(.degrees(particle.rotation))
                        .position(x: particle.x, y: particle.y)
                        .opacity(particle.opacity)
                }
            }
            .allowsHitTesting(false)
            .onChange(of: trigger) { isTriggered in
                if isTriggered {
                    spawnParticles(in: proxy.size)
                }
            }
        }
    }

    private func spawnParticles(in size: CGSize) {
        let colors: [Color] = [
            ColorTheme.primaryEmerald,
            ColorTheme.stageCyan,
            ColorTheme.stageAmber,
            .pink, .purple, .blue, .yellow
        ]

        var newParticles: [ConfettiParticle] = []
        for id in 0..<80 {
            let startX = size.width / 2.0 + CGFloat.random(in: -50...50)
            let startY = size.height * 0.35 + CGFloat.random(in: -30...30)
            let particle = ConfettiParticle(
                id: id,
                x: startX,
                y: startY,
                vx: CGFloat.random(in: -8...8),
                vy: CGFloat.random(in: -14 ... -4),
                rotation: Double.random(in: 0...360),
                rotationSpeed: Double.random(in: -12...12),
                size: CGSize(width: CGFloat.random(in: 6...12), height: CGFloat.random(in: 8...16)),
                color: colors.randomElement() ?? ColorTheme.primaryEmerald,
                opacity: 1.0
            )
            newParticles.append(particle)
        }
        self.particles = newParticles

        timer?.invalidate()
        let fps: Double = 60.0
        let dt: CGFloat = 1.0 / CGFloat(fps)
        let gravity: CGFloat = 28.0

        var framesRemaining = 120 // ~2 seconds
        timer = Timer.scheduledTimer(withTimeInterval: 1.0 / fps, repeats: true) { t in
            framesRemaining -= 1
            if framesRemaining <= 0 {
                t.invalidate()
                self.particles = []
                self.trigger = false
                return
            }

            for i in self.particles.indices {
                self.particles[i].vy += gravity * dt
                self.particles[i].x += self.particles[i].vx
                self.particles[i].y += self.particles[i].vy
                self.particles[i].rotation += self.particles[i].rotationSpeed
                if framesRemaining < 30 {
                    self.particles[i].opacity = Double(framesRemaining) / 30.0
                }
            }
        }
    }
}

private struct ConfettiParticle: Identifiable {
    let id: Int
    var x: CGFloat
    var y: CGFloat
    var vx: CGFloat
    var vy: CGFloat
    var rotation: Double
    var rotationSpeed: Double
    var size: CGSize
    var color: Color
    var opacity: Double
}
