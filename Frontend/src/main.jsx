import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import App from './App';
import { publicRoutes } from './routes/publicRoutes';
import { parentRoutes } from './routes/parentRoutes';
import PublicLayout from './layouts/PublicLayout';
import PrivateLayout from './layouts/PrivateLayout';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        {/* Public routes */}
        <Route element={<PublicLayout />}>
          {publicRoutes.map((route, index) => (
            <Route key={index} path={route.path} element={route.element} />
          ))}
        </Route>

        {/* Private routes (Protected) */}
        <Route element={<PrivateLayout />}>
          {parentRoutes.map((route, index) => (
            <Route key={index} path={route.path} element={route.element} />
          ))}
          {/* Add adminRoutes, driverRoutes, studentRoutes here similarly */}
        </Route>
      </Routes>
    </BrowserRouter>
  </React.StrictMode>
);
