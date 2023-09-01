import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from "react-router-dom";
import { PrimeReactProvider } from 'primereact/api';
import App from './App';
import "primereact/resources/primereact.min.css";
import "primereact/resources/themes/soho-dark/theme.css";
import "primeicons/primeicons.css";
import "primeflex/primeflex.css";

const root = createRoot(document.getElementById("root"));
root.render(
    <React.StrictMode>
        <BrowserRouter>
            <PrimeReactProvider>
                <App />
            </PrimeReactProvider>
        </BrowserRouter>
    </React.StrictMode>
);