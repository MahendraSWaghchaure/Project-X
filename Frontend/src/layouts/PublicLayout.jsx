//  Navbar/Footer for public pages

import { Outlet } from "react-router-dom";
import Header from "../components/Header";
import Footer from "../components/Footer";

const PublicLayout = () => (
  <>
    <Header />
    <main className="p-4">
      <Outlet />
    </main>
    <Footer />
  </>
);

export default PublicLayout;
