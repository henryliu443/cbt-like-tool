import SwiftUI

class MyState: ObservableObject {
    init() {
        print("MyState initialized")
    }
}

struct MyView: View {
    @StateObject var state: MyState
    init() {
        print("MyView initialized")
        let m = MyState()
        _state = StateObject(wrappedValue: m)
    }
    var body: some View {
        Text("Hello")
    }
}

let v = MyView()
let v2 = MyView()
