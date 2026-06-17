import Foundation

class Leaker {
    let id: Int
    var task: Task<Void, Never>?
    
    init(id: Int) {
        self.id = id
        print("Leaker \(id) init")
    }
    
    func start() {
        task = Task {
            print("Task started for \(id)")
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 100_000_000)
            }
            print("Task cancelled for \(id)")
        }
    }
    
    deinit {
        print("Leaker \(id) deinit")
        task?.cancel()
    }
}

var leaker: Leaker? = Leaker(id: 1)
leaker?.start()

DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
    print("Setting leaker to nil")
    leaker = nil
}

RunLoop.main.run(until: Date().addingTimeInterval(1.0))
