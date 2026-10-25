import React, { useState, useEffect } from 'react';
import {
  Train, LayoutDashboard, Ticket, CreditCard, DoorOpen, Database,
  Map as MapIcon, Flame, Bell, CheckCircle, AlertTriangle, RefreshCw
} from 'lucide-react';

import { InteractiveMap } from './components/InteractiveMap';
import { TicketBooking } from './components/TicketBooking';
import { SmartCards } from './components/SmartCards';
import { TurnstileGate } from './components/TurnstileGate';
import { AdminConsole } from './components/AdminConsole';
import { AnalyticsDashboard } from './components/AnalyticsDashboard';

export default function App() {
  const [activeTab, setActiveTab] = useState('map'); // 'map' | 'tickets' | 'cards' | 'gates' | 'dashboard' | 'admin'

  // Global Backend Data State
  const [stations, setStations] = useState([]);
  const [lines, setLines] = useState({});
  const [cards, setCards] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [stats, setStats] = useState(null);
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);

  // Cross-Component Routing State (Interactive Map -> Ticket Booking)
  const [origin, setOrigin] = useState(null);
  const [destination, setDestination] = useState(null);

  // Toast Notifications State
  const [toasts, setToasts] = useState([]);

  const addToast = (message, type = 'info') => {
    const id = Date.now() + Math.random();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 4500);
  };

  // Initial Fetch & Poll Data
  const fetchAllData = async () => {
    try {
      const [stRes, linesRes, cardsRes, txnRes, statsRes, statusRes] = await Promise.all([
        fetch('/api/stations').then(r => r.json()),
        fetch('/api/stations/lines').then(r => r.json()),
        fetch('/api/cards').then(r => r.json()),
        fetch('/api/admin/transactions').then(r => r.json()),
        fetch('/api/admin/stats').then(r => r.json()),
        fetch('/api/status').then(r => r.json())
      ]);

      setStations(stRes || []);
      setLines(linesRes || {});
      setCards(cardsRes || []);
      setTransactions(txnRes || []);
      setStats(statsRes || null);
      setStatus(statusRes || null);
    } catch (err) {
      console.error('Failed to fetch metro data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAllData();
    const interval = setInterval(async () => {
      try {
        const sRes = await fetch('/api/status').then(r => r.json());
        setStatus(sRes);
      } catch (e) {}
    }, 8000);
    return () => clearInterval(interval);
  }, []);

  // When a station is clicked on map
  const handleMapSelectStation = (station) => {
    if (!origin || (origin && destination)) {
      setOrigin(station);
      setDestination(null);
      addToast(`Selected ${station.name} (${station.lineName || station.lineCode}) as Origin`, 'info');
    } else {
      if (origin.stationId === station.stationId) {
        addToast('Origin and Destination cannot be the same station', 'error');
        return;
      }
      if (origin.lineCode !== station.lineCode) {
        addToast(`Cannot select stations from two different lines! Origin is on ${origin.lineName || origin.lineCode}. Please select a destination on the same line.`, 'error');
        return;
      }
      setDestination(station);
      addToast(`Selected ${station.name} as Destination. Ready to book!`, 'success');
      setActiveTab('tickets');
    }
  };

  const handleClearRoute = () => {
    setOrigin(null);
    setDestination(null);
    addToast('Route selection cleared', 'info');
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Top Navbar */}
      <header style={{
        position: 'sticky', top: 0, zIndex: 100,
        backgroundColor: '#000000',
        borderBottom: '1px solid rgba(255, 255, 255, 0.12)'
      }}>
        <div style={{ maxWidth: '1440px', margin: '0 auto', padding: '0.85rem 1.5rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          {/* Brand */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem', cursor: 'pointer' }} onClick={() => setActiveTab('map')}>
            <div style={{
              width: '42px', height: '42px', borderRadius: '12px',
              background: 'linear-gradient(135deg, #0284c7 0%, #2563eb 100%)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              boxShadow: '0 0 20px rgba(37,99,235,0.5)'
            }}>
              <Train style={{ color: 'white' }} size={24} />
            </div>
            <div>
              <div style={{ fontSize: '1.1rem', fontWeight: 800, letterSpacing: '-0.02em', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <span>MUMBAI METRO ONE</span>
                <span className="badge-pill" style={{ background: 'rgba(0, 240, 255, 0.15)', color: 'var(--accent-cyan)', border: '1px solid rgba(0, 240, 255, 0.3)', fontSize: '0.65rem' }}>
                  AMOLED PURE BLACK
                </span>
              </div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>
                Spring Boot 3.3.5 • SQLite RDBMS • 7 Lines (90 Stations)
              </div>
            </div>
          </div>

          {/* Navigation Tabs */}
          <nav style={{ display: 'flex', gap: '0.35rem', background: '#0a0a0a', padding: '0.35rem', borderRadius: '0.85rem', border: '1px solid rgba(255,255,255,0.1)' }}>
            {[
              { id: 'map', label: 'Interactive Map', icon: <MapIcon size={16} /> },
              { id: 'tickets', label: 'QR Tickets', icon: <Ticket size={16} /> },
              { id: 'cards', label: 'Smart Cards', icon: <CreditCard size={16} /> },
              { id: 'gates', label: 'AFC Turnstiles', icon: <DoorOpen size={16} /> },
              { id: 'dashboard', label: 'Analytics', icon: <LayoutDashboard size={16} /> },
              { id: 'admin', label: 'RDBMS Console', icon: <Database size={16} /> },
            ].map(tab => {
              const active = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className="btn-transit"
                  style={{
                    padding: '0.5rem 0.9rem', fontSize: '0.825rem',
                    background: active ? 'linear-gradient(135deg, #0284c7 0%, #2563eb 100%)' : 'transparent',
                    color: active ? '#ffffff' : 'var(--text-muted)',
                    boxShadow: active ? '0 0 16px rgba(37,99,235,0.5)' : 'none'
                  }}
                >
                  {tab.icon}
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </nav>

          {/* Peak Hour Background Daemon Status Badge */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
            <div style={{
              display: 'flex', alignItems: 'center', gap: '0.5rem',
              padding: '0.4rem 0.85rem', borderRadius: '9999px',
              background: status?.isPeakHour ? 'rgba(239,68,68,0.18)' : 'rgba(16,185,129,0.15)',
              border: `1px solid ${status?.isPeakHour ? '#ef4444' : '#10b981'}`
            }}>
              <Flame size={15} style={{ color: status?.isPeakHour ? '#ef4444' : '#10b981' }} />
              <div style={{ fontSize: '0.75rem', fontWeight: 700, color: status?.isPeakHour ? '#fca5a5' : '#86efac' }}>
                {status?.isPeakHour ? 'PEAK RUSH SURCHARGE (+₹5)' : 'STANDARD FARE HOURS'}
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main style={{ flex: 1, maxWidth: '1440px', width: '100%', margin: '0 auto', padding: '2rem 1.5rem' }}>
        {loading ? (
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: '50vh' }}>
            <div style={{ width: '48px', height: '48px', border: '3px solid rgba(56,189,248,0.2)', borderTopColor: '#38bdf8', borderRadius: '50%', animation: 'spin 1s linear infinite' }} />
            <p style={{ marginTop: '1rem', color: 'var(--text-muted)' }}>Connecting to Mumbai Metro Spring Boot Service...</p>
          </div>
        ) : (
          <>
            {activeTab === 'map' && (
              <InteractiveMap
                stations={stations}
                lines={lines}
                origin={origin}
                destination={destination}
                onSelectStation={handleMapSelectStation}
                onClearRoute={handleClearRoute}
              />
            )}

            {activeTab === 'tickets' && (
              <TicketBooking
                stations={stations}
                origin={origin}
                destination={destination}
                setOrigin={setOrigin}
                setDestination={setDestination}
                addToast={addToast}
              />
            )}

            {activeTab === 'cards' && (
              <SmartCards
                cards={cards}
                onRefreshCards={fetchAllData}
                addToast={addToast}
              />
            )}

            {activeTab === 'gates' && (
              <TurnstileGate
                cards={cards}
                stations={stations}
                onRefreshCards={fetchAllData}
                addToast={addToast}
              />
            )}

            {activeTab === 'dashboard' && (
              <AnalyticsDashboard
                stats={stats}
                transactions={transactions}
                lines={lines}
              />
            )}

            {activeTab === 'admin' && (
              <AdminConsole
                stats={stats}
                addToast={addToast}
              />
            )}
          </>
        )}
      </main>

      {/* Footer */}
      <footer style={{
        borderTop: '1px solid rgba(255,255,255,0.1)',
        background: '#000000',
        padding: '1.5rem 0',
        textAlign: 'center',
        fontSize: '0.8rem',
        color: 'var(--text-dim)'
      }}>
        <div>© 2026 Mumbai Metro One (MMOPL) - Smart Card & Fare Ticketing System</div>
        <div style={{ marginTop: '0.25rem', color: 'var(--text-muted)' }}>
          SE Sem 3 OOPM Mini Project • Full-Stack React + Spring Boot 3.3.5 Web Architecture
        </div>
      </footer>

      {/* Toast Notification Container */}
      <div className="toast-container">
        {toasts.map(t => (
          <div key={t.id} className="toast-item" style={{
            borderColor: t.type === 'error' ? '#ef4444' : t.type === 'success' ? '#10b981' : 'var(--accent-cyan)'
          }}>
            {t.type === 'error' ? <AlertTriangle style={{ color: '#ef4444', flexShrink: 0 }} size={20} /> :
             t.type === 'success' ? <CheckCircle style={{ color: '#10b981', flexShrink: 0 }} size={20} /> :
             <Bell style={{ color: 'var(--accent-cyan)', flexShrink: 0 }} size={20} />}
            <div style={{ fontSize: '0.85rem', fontWeight: 500, lineHeight: 1.3 }}>{t.message}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
