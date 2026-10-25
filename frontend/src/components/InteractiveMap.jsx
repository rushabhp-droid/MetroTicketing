import React, { useState } from 'react';
import { Train, Navigation, MapPin, Compass, Info } from 'lucide-react';

const LINE_CONFIG = {
  LINE_1: { name: 'Line 1 (Blue)', color: '#2563eb', bg: 'rgba(37,99,235,0.15)' },
  LINE_2A: { name: 'Line 2A (Yellow)', color: '#eab308', bg: 'rgba(234,179,8,0.15)' },
  LINE_7: { name: 'Line 7 (Red)', color: '#ef4444', bg: 'rgba(239,68,68,0.15)' },
  LINE_9: { name: 'Line 9 (Red Ext)', color: '#f43f5e', bg: 'rgba(244,63,94,0.15)' },
  LINE_3: { name: 'Line 3 (Aqua)', color: '#06b6d4', bg: 'rgba(6,182,212,0.15)' },
  LINE_2B: { name: 'Line 2B (Yellow Ext)', color: '#f59e0b', bg: 'rgba(245,158,11,0.15)' },
  NMM_1: { name: 'Navi Mumbai L1', color: '#8b5cf6', bg: 'rgba(139,92,246,0.15)' },
};

export function InteractiveMap({ stations, lines, origin, destination, onSelectStation, onClearRoute }) {
  const [activeLineFilter, setActiveLineFilter] = useState('ALL');
  const [hoveredStation, setHoveredStation] = useState(null);

  const filteredStations = activeLineFilter === 'ALL'
    ? stations
    : stations.filter(s => s.lineCode === activeLineFilter);

  // Group stations by line for schematics
  const lineKeys = Object.keys(LINE_CONFIG);

  return (
    <div className="glass-panel" style={{ padding: '1.5rem', marginBottom: '2rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.25rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{
            width: '42px', height: '42px', borderRadius: '12px',
            background: 'linear-gradient(135deg, #0284c7 0%, #2563eb 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center'
          }}>
            <Train style={{ color: 'white' }} size={22} />
          </div>
          <div>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700, letterSpacing: '-0.02em' }}>
              Interactive Mumbai Metro Network Map
            </h2>
            <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
              Click any station node below to set Origin or Destination route
            </p>
          </div>
        </div>

        {/* Line Filter Tabs */}
        <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
          <button
            onClick={() => setActiveLineFilter('ALL')}
            className="btn-transit"
            style={{
              padding: '0.4rem 0.85rem', fontSize: '0.75rem',
              background: activeLineFilter === 'ALL' ? 'var(--accent-blue)' : 'rgba(255,255,255,0.06)',
              color: activeLineFilter === 'ALL' ? 'white' : 'var(--text-muted)'
            }}
          >
            All Lines (90)
          </button>
          {lineKeys.map(key => {
            const cfg = LINE_CONFIG[key];
            const active = activeLineFilter === key;
            return (
              <button
                key={key}
                onClick={() => setActiveLineFilter(key)}
                className="btn-transit"
                style={{
                  padding: '0.4rem 0.75rem', fontSize: '0.75rem',
                  border: `1px solid ${cfg.color}`,
                  background: active ? cfg.color : cfg.bg,
                  color: active ? '#ffffff' : cfg.color,
                  fontWeight: active ? 700 : 500
                }}
              >
                {cfg.name}
              </button>
            );
          })}
        </div>
      </div>

      {/* Network Station Grid Visualization */}
      <div style={{
        background: '#000000',
        borderRadius: '1rem',
        border: '1px solid rgba(255,255,255,0.12)',
        padding: '1.5rem',
        minHeight: '280px',
        position: 'relative',
        overflow: 'hidden'
      }}>
        {/* Ambient Grid Lines Background */}
        <div style={{
          position: 'absolute', inset: 0,
          backgroundImage: 'linear-gradient(rgba(255,255,255,0.02) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.02) 1px, transparent 1px)',
          backgroundSize: '32px 32px',
          pointerEvents: 'none'
        }} />

        {/* Selected Route Info Banner */}
        {(origin || destination) && (
          <div style={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            flexWrap: 'wrap', gap: '0.75rem',
            background: '#080808',
            border: '1px solid rgba(0, 240, 255, 0.35)',
            borderRadius: '0.75rem', padding: '0.75rem 1.25rem',
            marginBottom: '1.25rem',
            boxShadow: '0 4px 16px rgba(0,0,0,0.8)'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <span className="badge-pill" style={{ background: 'rgba(16,185,129,0.2)', color: '#10b981', border: '1px solid #10b981' }}>
                  <MapPin size={12} /> Origin
                </span>
                <strong style={{ fontSize: '0.9rem' }}>{origin ? `${origin.name} (${origin.lineName || origin.lineCode})` : 'Select on map...'}</strong>
              </div>
              <span style={{ color: 'var(--text-dim)' }}>➔</span>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <span className="badge-pill" style={{ background: 'rgba(239,68,68,0.2)', color: '#ef4444', border: '1px solid #ef4444' }}>
                  <Navigation size={12} /> Destination
                </span>
                <strong style={{ fontSize: '0.9rem' }}>{destination ? `${destination.name} (${destination.lineName || destination.lineCode})` : 'Select destination on same line...'}</strong>
              </div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              {origin && destination ? (
                <div style={{ fontSize: '0.85rem', color: 'var(--accent-cyan)', fontWeight: 600 }}>
                  Same-Line Journey Route Active
                </div>
              ) : origin ? (
                <div style={{ fontSize: '0.82rem', color: '#f59e0b', fontWeight: 600 }}>
                  Select destination on {origin.lineName || origin.lineCode}
                </div>
              ) : null}
              {onClearRoute && (
                <button
                  onClick={onClearRoute}
                  className="btn-transit"
                  style={{
                    padding: '0.35rem 0.75rem',
                    fontSize: '0.75rem',
                    background: 'rgba(239, 68, 68, 0.15)',
                    border: '1px solid rgba(239, 68, 68, 0.4)',
                    color: '#fca5a5'
                  }}
                  title="Clear selected route stations"
                >
                  ✕ Clear Selection
                </button>
              )}
            </div>
          </div>
        )}

        {/* Render Lines with Connected Nodes */}
        {lineKeys.map(lineKey => {
          if (activeLineFilter !== 'ALL' && activeLineFilter !== lineKey) return null;
          const lineStations = stations.filter(s => s.lineCode === lineKey);
          if (!lineStations.length) return null;
          const cfg = LINE_CONFIG[lineKey] || { name: lineKey, color: '#38bdf8' };

          return (
            <div key={lineKey} style={{ marginBottom: '1.5rem', position: 'relative' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
                <span style={{
                  display: 'inline-block', width: '12px', height: '12px', borderRadius: '50%',
                  backgroundColor: cfg.color, boxShadow: `0 0 10px ${cfg.color}`
                }} />
                <h4 style={{ fontSize: '0.9rem', fontWeight: 700, color: cfg.color }}>
                  {cfg.name} <span style={{ color: 'var(--text-dim)', fontSize: '0.75rem' }}>({lineStations.length} stations)</span>
                </h4>
              </div>

              {/* Station Pills Row with Connecting Track Line */}
              <div style={{
                display: 'flex', gap: '0.65rem', overflowX: 'auto',
                paddingBottom: '0.75rem', position: 'relative', scrollbarWidth: 'thin'
              }}>
                {/* Visual Track Line */}
                <div style={{
                  position: 'absolute', top: '18px', left: '20px', right: '20px', height: '3px',
                  backgroundColor: cfg.color, opacity: 0.35, zIndex: 0
                }} />

                {lineStations.map((st, idx) => {
                  const isOrigin = origin && origin.stationId === st.stationId;
                  const isDest = destination && destination.stationId === st.stationId;
                  const hasInterchange = st.interchange && st.interchange !== 'None';
                  const isDifferentLine = origin && !destination && st.lineCode !== origin.lineCode;

                  let ringStyle = {};
                  if (isOrigin) {
                    ringStyle = { borderColor: '#10b981', boxShadow: '0 0 15px #10b981', background: 'rgba(16,185,129,0.25)' };
                  } else if (isDest) {
                    ringStyle = { borderColor: '#ef4444', boxShadow: '0 0 15px #ef4444', background: 'rgba(239,68,68,0.25)' };
                  } else if (isDifferentLine) {
                    ringStyle = { opacity: 0.35, filter: 'grayscale(60%)', cursor: 'not-allowed' };
                  }

                  return (
                    <div
                      key={st.stationId}
                      onClick={() => onSelectStation(st)}
                      onMouseEnter={() => setHoveredStation(st)}
                      onMouseLeave={() => setHoveredStation(null)}
                      title={isDifferentLine ? `Cannot select: Different line from origin (${origin.lineName || origin.lineCode})` : st.interchange || st.name}
                      style={{
                        zIndex: 1,
                        cursor: isDifferentLine ? 'not-allowed' : 'pointer',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        minWidth: '120px',
                        maxWidth: '140px',
                        padding: '0.5rem 0.65rem',
                        background: '#050505',
                        border: '1px solid rgba(255,255,255,0.12)',
                        borderRadius: '0.75rem',
                        transition: 'all 0.2s ease',
                        textAlign: 'center',
                        ...ringStyle
                      }}
                    >
                      {/* Node Dot on track */}
                      <div style={{
                        width: '14px', height: '14px', borderRadius: '50%',
                        backgroundColor: isOrigin ? '#10b981' : isDest ? '#ef4444' : cfg.color,
                        boxShadow: `0 0 8px ${isOrigin ? '#10b981' : isDest ? '#ef4444' : cfg.color}`,
                        marginBottom: '0.45rem',
                        border: '2px solid #000000'
                      }} />

                      <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-main)', lineHeight: 1.2 }}>
                        {st.name}
                      </span>
                      <span style={{ fontSize: '0.68rem', color: 'var(--text-dim)', marginTop: '0.2rem' }}>
                        {st.distanceKm.toFixed(1)} km
                      </span>

                      {hasInterchange && (
                        <span style={{
                          marginTop: '0.35rem',
                          fontSize: '0.62rem',
                          padding: '0.15rem 0.4rem',
                          borderRadius: '4px',
                          background: 'rgba(6,182,212,0.18)',
                          color: '#38bdf8',
                          fontWeight: 600,
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          maxWidth: '100%'
                        }} title={st.interchange}>
                          ⇄ {st.interchange.split(' ')[0]}
                        </span>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
