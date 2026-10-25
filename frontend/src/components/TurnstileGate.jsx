import React, { useState } from 'react';
import { DoorOpen, ShieldCheck, AlertTriangle, ArrowRight, Wallet, CheckCircle, RefreshCw } from 'lucide-react';
import confetti from 'canvas-confetti';

export function TurnstileGate({ cards, stations, onRefreshCards, addToast }) {
  const [selectedCardId, setSelectedCardId] = useState(cards[0]?.cardId || '');
  const [entryStation, setEntryStation] = useState(stations[0]?.stationId || '');
  const [exitStation, setExitStation] = useState(stations[1]?.stationId || '');
  const [isPeakHour, setIsPeakHour] = useState(false);
  const [gateState, setGateState] = useState('STANDBY'); // STANDBY | OPEN | DENIED
  const [gateMessage, setGateMessage] = useState('Hold Smart Card near RFID Reader to pass turnstile gate');
  const [lastTxn, setLastTxn] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const selectedCard = cards.find(c => c.cardId === selectedCardId);

  // Trigger Tap-In
  const handleTapIn = async () => {
    if (!selectedCardId || !entryStation) {
      addToast('Please choose a card and entry station', 'error');
      return;
    }
    setIsProcessing(true);
    try {
      const res = await fetch('/api/cards/tap-in', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cardId: selectedCardId, station: String(entryStation) })
      });
      const data = await res.json();
      if (data.success) {
        setGateState('OPEN');
        setGateMessage(data.message);
        setLastTxn(data);
        addToast(data.message, 'success');
        onRefreshCards();
        setTimeout(() => setGateState('STANDBY'), 3500);
      } else {
        setGateState('DENIED');
        setGateMessage(data.message);
        addToast(data.message, 'error');
        setTimeout(() => setGateState('STANDBY'), 3500);
      }
    } catch (e) {
      setGateState('DENIED');
      setGateMessage('Network communication failure: ' + e.message);
      addToast('Gate connection error', 'error');
      setTimeout(() => setGateState('STANDBY'), 3500);
    } finally {
      setIsProcessing(false);
    }
  };

  // Trigger Tap-Out
  const handleTapOut = async () => {
    if (!selectedCardId || !exitStation) {
      addToast('Please choose a card and exit station', 'error');
      return;
    }
    setIsProcessing(true);
    try {
      const res = await fetch('/api/cards/tap-out', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          cardId: selectedCardId,
          station: String(exitStation),
          peakHour: isPeakHour
        })
      });
      const data = await res.json();
      if (data.success) {
        setGateState('OPEN');
        setGateMessage(data.message);
        setLastTxn(data);
        addToast(data.message, 'success');
        confetti({ particleCount: 30, spread: 60, origin: { y: 0.7 } });
        onRefreshCards();
        setTimeout(() => setGateState('STANDBY'), 4000);
      } else {
        setGateState('DENIED');
        setGateMessage(data.message);
        addToast(data.message, 'error');
        setTimeout(() => setGateState('STANDBY'), 3500);
      }
    } catch (e) {
      setGateState('DENIED');
      setGateMessage('Network error: ' + e.message);
      addToast('Gate connection error', 'error');
      setTimeout(() => setGateState('STANDBY'), 3500);
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <div className="glass-panel" style={{ padding: '2rem', marginBottom: '2rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
        <div style={{
          width: '42px', height: '42px', borderRadius: '12px',
          background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <DoorOpen style={{ color: 'white' }} size={22} />
        </div>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>
            Automated Fare Collection (AFC) Turnstile Gate Simulator
          </h2>
          <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
            Physical motorized gate barrier physics, RFID sensor, and SQLite relational deduction engine
          </p>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '2rem' }}>
        {/* Interactive 3D Gate Viewport */}
        <div className="turnstile-box" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'space-between', minHeight: '380px' }}>
          {/* Overhead LED Indicator Bar */}
          <div style={{ width: '100%', marginBottom: '1.5rem', textAlign: 'center' }}>
            <div className={`gate-led-strip ${gateState === 'OPEN' ? 'gate-led-pass' : gateState === 'DENIED' ? 'gate-led-stop' : 'gate-led-standby'}`} />
            <div style={{
              marginTop: '0.75rem',
              fontSize: '0.825rem',
              fontWeight: 700,
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
              color: gateState === 'OPEN' ? '#10b981' : gateState === 'DENIED' ? '#ef4444' : '#f59e0b'
            }}>
              {gateState === 'OPEN' ? '● ACCESS GRANTED - PROCEED' : gateState === 'DENIED' ? '✕ ACCESS DENIED - INSUFFICIENT BALANCE' : '◈ GATE STANDBY - TAP SMART CARD'}
            </div>
          </div>

          {/* Motorized Barrier Flaps */}
          <div style={{
            width: '100%',
            maxWidth: '300px',
            height: '140px',
            position: 'relative',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'flex-end',
            padding: '0 1rem',
            borderBottom: '3px solid #334155'
          }}>
            {/* Left Barrier */}
            <div className={`gate-barrier left ${gateState === 'OPEN' ? 'open-left' : ''}`} />
            {/* Turnstile Passage Center Sensor */}
            <div style={{
              width: '40px', height: '40px', borderRadius: '50%',
              background: 'radial-gradient(circle, rgba(56,189,248,0.4) 0%, transparent 70%)',
              marginBottom: '20px',
              animation: 'pulseGlow 2s infinite'
            }} />
            {/* Right Barrier */}
            <div className={`gate-barrier right ${gateState === 'OPEN' ? 'open-right' : ''}`} />
          </div>

          {/* RFID Sensor Tap Pad */}
          <div style={{
            marginTop: '1.5rem',
            padding: '1rem 1.5rem',
            background: 'rgba(30, 41, 59, 0.7)',
            border: '1px dashed rgba(56, 189, 248, 0.4)',
            borderRadius: '1rem',
            textAlign: 'center',
            width: '100%',
            maxWidth: '340px'
          }}>
            <div style={{ fontSize: '0.8rem', color: 'var(--accent-cyan)', fontWeight: 600, marginBottom: '0.25rem' }}>
              (( • )) NFC / RFID SMART CARD SENSOR
            </div>
            <div style={{ fontSize: '0.825rem', color: 'var(--text-main)', fontWeight: 500 }}>
              {gateMessage}
            </div>
          </div>

          {/* Last Transaction Chip */}
          {lastTxn && (
            <div style={{
              marginTop: '1rem',
              fontSize: '0.75rem',
              color: 'var(--text-muted)',
              background: 'rgba(0,0,0,0.4)',
              padding: '0.4rem 0.85rem',
              borderRadius: '9999px'
            }}>
              {lastTxn.cardId} | {lastTxn.holderName} | Balance: ₹{lastTxn.balanceAfter ?? lastTxn.balance}
            </div>
          )}
        </div>

        {/* Turnstile Control Kiosk */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {/* Card Selection */}
          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
              Select Passenger Smart Card (Polymorphic)
            </label>
            <select
              value={selectedCardId}
              onChange={e => setSelectedCardId(e.target.value)}
              className="select-transit"
            >
              {cards.map(c => (
                <option key={c.cardId} value={c.cardId}>
                  {c.cardId} - {c.holderName} (₹{c.balance.toFixed(2)}) {c.blocked ? '[BLOCKED]' : c.inTransit ? '[IN TRANSIT]' : ''}
                </option>
              ))}
            </select>
          </div>

          {/* Selected Card Live Status Preview */}
          {selectedCard && (
            <div style={{
              background: selectedCard.blocked ? 'rgba(239,68,68,0.1)' : 'rgba(15,23,42,0.6)',
              border: `1px solid ${selectedCard.blocked ? '#ef4444' : 'rgba(255,255,255,0.1)'}`,
              borderRadius: '0.75rem', padding: '0.85rem 1.15rem',
              display: 'flex', justifyContent: 'space-between', alignItems: 'center'
            }}>
              <div>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>COMMUTER DETAILS</div>
                <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>{selectedCard.holderName}</div>
                <div style={{ fontSize: '0.75rem', color: 'var(--accent-cyan)' }}>
                  {selectedCard.cardId} • {selectedCard.inTransit ? `In Transit from ${selectedCard.tapInStation?.name || 'Gate'}` : 'Not In Transit'}
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-dim)' }}>BALANCE</div>
                <div style={{ fontSize: '1.2rem', fontWeight: 800, color: selectedCard.balance < 50 ? '#ef4444' : '#10b981' }}>
                  ₹{selectedCard.balance.toFixed(2)}
                </div>
              </div>
            </div>
          )}

          {/* Entry Gate Operation */}
          <div style={{ background: 'rgba(255,255,255,0.03)', padding: '1rem', borderRadius: '0.85rem', border: '1px solid rgba(255,255,255,0.06)' }}>
            <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--accent-emerald)', marginBottom: '0.65rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <ArrowRight size={16} /> AFC Entry Gate (Tap-In)
            </div>
            <div style={{ display: 'flex', gap: '0.75rem' }}>
              <select
                value={entryStation}
                onChange={e => setEntryStation(e.target.value)}
                className="select-transit"
                style={{ flex: 2 }}
              >
                {stations.map(st => (
                  <option key={st.stationId} value={st.stationId}>
                    {st.name} ({st.lineName})
                  </option>
                ))}
              </select>
              <button
                onClick={handleTapIn}
                disabled={isProcessing}
                className="btn-transit btn-emerald"
                style={{ whiteSpace: 'nowrap' }}
              >
                Tap-In Gate
              </button>
            </div>
          </div>

          {/* Exit Gate Operation */}
          <div style={{ background: 'rgba(255,255,255,0.03)', padding: '1rem', borderRadius: '0.85rem', border: '1px solid rgba(255,255,255,0.06)' }}>
            <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--accent-rose)', marginBottom: '0.65rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <ArrowRight size={16} /> AFC Exit Gate (Tap-Out & Deduction)
            </div>
            <div style={{ display: 'flex', gap: '0.75rem', marginBottom: '0.75rem' }}>
              <select
                value={exitStation}
                onChange={e => setExitStation(e.target.value)}
                className="select-transit"
                style={{ flex: 2 }}
              >
                {stations.map(st => (
                  <option key={st.stationId} value={st.stationId}>
                    {st.name} ({st.lineName})
                  </option>
                ))}
              </select>
              <button
                onClick={handleTapOut}
                disabled={isProcessing}
                className="btn-transit btn-danger"
                style={{ whiteSpace: 'nowrap' }}
              >
                Tap-Out Gate
              </button>
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8rem', color: 'var(--text-muted)', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={isPeakHour}
                onChange={e => setIsPeakHour(e.target.checked)}
              />
              Simulate Peak-Hour Rush (+₹5.00 surcharge)
            </label>
          </div>
        </div>
      </div>
    </div>
  );
}
