import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import PublicLayout from "./layouts/PublicLayout";
import Home from "./pages/public/Home";
import About from "./pages/public/About";
import Contact from "./pages/public/Contact";
//import Login from "./pages/auth/Login"; // Example auth page

function App() {
  return (
    <Router>
      <Routes>
        {/* Public Layout with nested routes */}
        <Route element={<PublicLayout />}>
          <Route path="/" element={<Home />} />         {/* ✅ This will be the first page shown */}
          <Route path="/about" element={<About />} />
          <Route path="/contact" element={<Contact />} />
        </Route>

        {/* Auth or role-specific routes can be added here */}
        <Route path="/login" element={<Login />} />
      </Routes>
    </Router>
  );
}

export default App;
