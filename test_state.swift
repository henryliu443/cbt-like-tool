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
        _state = StateObject(wrappedValue: MyState())
    }
    var body: some View {
        Text("Hello")
    }
}

let v = MyView()
let v2 = MyView()
