import React, { useState, useEffect } from 'react';
import { Ticket as TicketIcon, QrCode, Sparkles, Printer, ArrowRight, ShieldCheck, Clock, Users } from 'lucide-react';
import confetti from 'canvas-confetti';

export function TicketBooking({ stations, origin, destination, setOrigin, setDestination, addToast }) {
  const [sourceId, setSourceId] = useState(origin?.stationId || stations[0]?.stationId || '');
  const [destId, setDestId] = useState(destination?.stationId || stations[11]?.stationId || '');
  const [passengers, setPassengers] = useState(1);
  const [returnTrip, setReturnTrip] = useState(false);
  const [peakHour, setPeakHour] = useState(false);

  // Live Fare Preview State
  const [farePreview, setFarePreview] = useState(null);
  const [isCalculating, setIsCalculating] = useState(false);
  const [bookedTicket, setBookedTicket] = useState(null);
  const [isBooking, setIsBooking] = useState(false);

  // Sync with prop changes from Interactive Map clicks
  useEffect(() => {
    if (origin) setSourceId(origin.stationId);
  }, [origin]);

  useEffect(() => {
    if (destination) setDestId(destination.stationId);
  }, [destination]);

  // Fetch Live Fare Preview
  useEffect(() => {
    if (!sourceId || !destId || sourceId === destId) {
      setFarePreview(null);
      return;
    }

    const timer = setTimeout(async () => {
      setIsCalculating(true);
      try {
        const url = `/api/stations/fare-preview?source=${sourceId}&dest=${destId}&passengers=${passengers}&isReturn=${returnTrip}&isPeakHour=${peakHour}`;
        const res = await fetch(url);
        const data = await res.json();
        if (res.ok) {
          setFarePreview(data);
        } else {
          setFarePreview(null);
        }
      } catch (e) {
        setFarePreview(null);
      } finally {
        setIsCalculating(false);
      }
    }, 150);

    return () => clearTimeout(timer);
  }, [sourceId, destId, passengers, returnTrip, peakHour]);

  const handleBookTicket = async (e) => {
    e.preventDefault();
    if (sourceId === destId) {
      addToast('Origin and Destination stations cannot be the same!', 'error');
      return;
    }
    setIsBooking(true);
    try {
      const res = await fetch('/api/tickets/book', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          source: String(sourceId),
          destination: String(destId),
          passengers,
          returnTrip,
          peakHour
        })
      });
      const data = await res.json();
      if (data.success) {
        setBookedTicket(data);
        addToast(`Ticket ${data.ticketId} booked successfully!`, 'success');
        confetti({
          particleCount: 80,
          spread: 70,
          origin: { y: 0.6 }
        });
      } else {
        addToast(data.message || 'Booking failed', 'error');
      }
    } catch (err) {
      addToast('Network error while booking: ' + err.message, 'error');
    } finally {
      setIsBooking(false);
    }
  };

  return (
    <div style={{ marginBottom: '2rem' }}>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '2rem' }}>
        {/* Booking Form Panel */}
        <div className="glass-panel" style={{ padding: '2rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
            <div style={{
              width: '42px', height: '42px', borderRadius: '12px',
              background: 'linear-gradient(135deg, #0284c7 0%, #2563eb 100%)',
              display: 'flex', alignItems: 'center', justifyContent: 'center'
            }}>
              <TicketIcon style={{ color: 'white' }} size={22} />
            </div>
            <div>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Single / Return QR Journey Booking</h2>
              <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
                Real-time fare calculation, pure-Java Reed-Solomon Base64 QR code generation
              </p>
            </div>
          </div>

          <form onSubmit={handleBookTicket} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {/* Origin Station */}
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
                Origin (Source) Station
              </label>
              <select
                value={sourceId}
                onChange={e => {
                  setSourceId(Number(e.target.value));
                  const st = stations.find(s => s.stationId === Number(e.target.value));
                  if (st) setOrigin(st);
                }}
                className="select-transit"
                required
              >
                {stations.map(st => (
                  <option key={st.stationId} value={st.stationId}>
                    {st.name} ({st.lineName})
                  </option>
                ))}
              </select>
            </div>

            {/* Destination Station */}
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
                Destination Station
              </label>
              <select
                value={destId}
                onChange={e => {
                  setDestId(Number(e.target.value));
                  const st = stations.find(s => s.stationId === Number(e.target.value));
                  if (st) setDestination(st);
                }}
                className="select-transit"
                required
              >
                {stations.map(st => (
                  <option key={st.stationId} value={st.stationId}>
                    {st.name} ({st.lineName})
                  </option>
                ))}
              </select>
            </div>

            {/* Passengers & Trip Controls */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
                  Passengers Count
                </label>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <button
                    type="button"
                    onClick={() => setPassengers(Math.max(1, passengers - 1))}
                    className="btn-transit btn-ghost"
                    style={{ padding: '0.5rem 0.85rem' }}
                  >
                    -
                  </button>
                  <input
                    type="number"
                    min="1"
                    max="10"
                    value={passengers}
                    onChange={e => setPassengers(Math.max(1, parseInt(e.target.value) || 1))}
                    className="input-transit"
                    style={{ textAlign: 'center', fontWeight: 700 }}
                  />
                  <button
                    type="button"
                    onClick={() => setPassengers(Math.min(10, passengers + 1))}
                    className="btn-transit btn-ghost"
                    style={{ padding: '0.5rem 0.85rem' }}
                  >
                    +
                  </button>
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
                  Trip Options
                </label>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginTop: '0.25rem' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8rem', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={returnTrip}
                      onChange={e => setReturnTrip(e.target.checked)}
                    />
                    <span>Return Trip <strong>(10% OFF)</strong></span>
                  </label>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.8rem', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={peakHour}
                      onChange={e => setPeakHour(e.target.checked)}
                    />
                    <span>Peak Rush Surcharge (+₹5)</span>
                  </label>
                </div>
              </div>
            </div>

            {/* Live Fare Calculation Box */}
            {farePreview && (
              <div style={{
                background: '#080808',
                border: '1px solid rgba(0, 240, 255, 0.35)',
                borderRadius: '0.85rem',
                padding: '1rem 1.25rem',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                boxShadow: '0 0 20px rgba(0, 240, 255, 0.1)'
              }}>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--accent-cyan)', fontWeight: 700, letterSpacing: '0.05em' }}>
                    LIVE FARE CALCULATION
                  </div>
                  <div style={{ fontSize: '0.875rem', fontWeight: 600, color: '#ffffff', marginTop: '0.2rem' }}>
                    {farePreview.source} ➔ {farePreview.destination} ({farePreview.distanceKm} km)
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)', marginTop: '0.15rem' }}>
                    Unit Fare: ₹{farePreview.baseFare.toFixed(2)} | Concession: ₹{farePreview.studentFare.toFixed(2)}
                  </div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>TOTAL FARE</div>
                  <div style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--accent-cyan)' }}>
                    ₹{farePreview.totalFare.toFixed(2)}
                  </div>
                </div>
              </div>
            )}

            <button
              type="submit"
              disabled={isBooking || sourceId === destId}
              className="btn-transit btn-primary"
              style={{ padding: '0.85rem', fontSize: '1rem', width: '100%', marginTop: '0.5rem' }}
            >
              <Sparkles size={18} />
              {isBooking ? 'Generating Pure-Java QR Token...' : `Book Token & Pay ₹${farePreview ? farePreview.totalFare.toFixed(2) : '--'}`}
            </button>
          </form>
        </div>

        {/* Generated QR Ticket Presentation Viewport */}
        <div>
          {bookedTicket ? (
            <div className="ticket-container" style={{ padding: '2rem' }}>
              <div className="ticket-punch-left" />
              <div className="ticket-punch-right" />

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '2px dashed rgba(255, 255, 255, 0.15)', paddingBottom: '1rem' }}>
                <div>
                  <div style={{ fontSize: '0.7rem', letterSpacing: '0.1em', fontWeight: 800, color: '#a1a1aa' }}>
                    MUMBAI METRO ONE (MMOPL)
                  </div>
                  <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#ffffff' }}>
                    {bookedTicket.ticketType}
                  </div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <span className="badge-pill" style={{ background: 'rgba(0, 255, 136, 0.1)', color: '#00ff88', border: '1px solid rgba(0, 255, 136, 0.3)' }}>
                    VALID DIGITAL TOKEN
                  </span>
                  <div style={{ fontSize: '0.75rem', color: '#a1a1aa', marginTop: '0.25rem' }}>
                    ID: {bookedTicket.ticketId}
                  </div>
                </div>
              </div>

              {/* Journey Details */}
              <div style={{ margin: '1.25rem 0', display: 'grid', gridTemplateColumns: '1fr auto 1fr', alignItems: 'center', gap: '0.75rem' }}>
                <div>
                  <div style={{ fontSize: '0.75rem', color: '#a1a1aa' }}>ORIGIN</div>
                  <div style={{ fontWeight: 800, fontSize: '1.05rem', color: '#ffffff' }}>{bookedTicket.source}</div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--accent-cyan)' }}>{bookedTicket.sourceLine}</div>
                </div>
                <ArrowRight style={{ color: 'var(--accent-cyan)' }} size={24} />
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '0.75rem', color: '#a1a1aa' }}>DESTINATION</div>
                  <div style={{ fontWeight: 800, fontSize: '1.05rem', color: '#ffffff' }}>{bookedTicket.destination}</div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--accent-cyan)' }}>{bookedTicket.destinationLine}</div>
                </div>
              </div>

              {/* QR Code Graphic with Laser Scanner Animation */}
              <div style={{
                position: 'relative',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                background: '#ffffff',
                padding: '1.25rem',
                borderRadius: '1rem',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                margin: '1.25rem 0'
              }}>
                <div className="laser-scanner" />
                <img
                  src={`data:image/png;base64,${bookedTicket.qrBase64}`}
                  alt="Scannable Pure-Java Metro QR Code"
                  style={{ width: '180px', height: '180px', borderRadius: '8px' }}
                />
                <div style={{
                  display: 'flex', alignItems: 'center', gap: '0.4rem',
                  fontSize: '0.75rem', fontWeight: 700, color: '#16a34a', marginTop: '0.75rem'
                }}>
                  <ShieldCheck size={16} /> Pure-Java Reed-Solomon GF(256) Verified
                </div>
                <div style={{ fontSize: '0.7rem', color: '#475569', marginTop: '0.15rem' }}>
                  Hold phone or printed barcode at AFC gate scanner
                </div>
              </div>

              {/* Ticket Footer Financials */}
              <div style={{
                display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                borderTop: '2px dashed rgba(255, 255, 255, 0.15)', paddingTop: '1rem', fontSize: '0.85rem'
              }}>
                <div>
                  <div style={{ color: '#a1a1aa', fontSize: '0.75rem' }}>PASSENGERS: {bookedTicket.passengers}</div>
                  <div style={{ color: '#a1a1aa', fontSize: '0.75rem' }}>DISTANCE: {bookedTicket.distanceKm} km</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ color: '#a1a1aa', fontSize: '0.75rem' }}>AMOUNT PAID</div>
                  <div style={{ fontSize: '1.35rem', fontWeight: 900, color: '#00f0ff' }}>
                    ₹{bookedTicket.totalAmount.toFixed(2)}
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div className="glass-panel" style={{
              padding: '3rem 2rem',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              minHeight: '380px',
              textAlign: 'center'
            }}>
              <QrCode size={64} style={{ color: 'var(--accent-cyan)', opacity: 0.5, marginBottom: '1rem' }} />
              <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '0.4rem' }}>
                Instant Pure-Java QR Ticket Preview
              </h3>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', maxWidth: '320px' }}>
                Select an origin and destination on the map or booking form to generate a live QR token and scannable barcode.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
