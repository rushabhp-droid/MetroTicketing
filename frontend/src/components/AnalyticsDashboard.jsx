import React from 'react';
import { IndianRupee, CreditCard, Activity, MapPin, TrendingUp, Clock } from 'lucide-react';

export function AnalyticsDashboard({ stats, transactions, lines }) {
  const trafficEntries = stats?.traffic ? Object.entries(stats.traffic).sort((a, b) => b[1] - a[1]) : [];
  const maxTraffic = trafficEntries.length > 0 ? Math.max(...trafficEntries.map(e => e[1]), 1) : 1;

  return (
    <div style={{ marginBottom: '2rem' }}>
      {/* 4 KPI Metrics */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem', marginBottom: '2rem' }}>
        <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{
            width: '48px', height: '48px', borderRadius: '14px',
            background: 'linear-gradient(135deg, #059669 0%, #10b981 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center'
          }}>
            <IndianRupee style={{ color: 'white' }} size={24} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Total Network Revenue</div>
            <div style={{ fontSize: '1.65rem', fontWeight: 800, color: 'var(--text-main)', letterSpacing: '-0.02em' }}>
              ₹{stats ? stats.totalRevenue.toFixed(2) : '0.00'}
            </div>
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{
            width: '48px', height: '48px', borderRadius: '14px',
            background: 'linear-gradient(135deg, #0284c7 0%, #38bdf8 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center'
          }}>
            <MapPin style={{ color: 'white' }} size={24} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Network Stations</div>
            <div style={{ fontSize: '1.65rem', fontWeight: 800, color: 'var(--text-main)' }}>
              {stats?.totalStations || 90}
            </div>
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{
            width: '48px', height: '48px', borderRadius: '14px',
            background: 'linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center'
          }}>
            <CreditCard style={{ color: 'white' }} size={24} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Issued Smart Cards</div>
            <div style={{ fontSize: '1.65rem', fontWeight: 800, color: 'var(--text-main)' }}>
              {stats?.totalCards || 0}
            </div>
          </div>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{
            width: '48px', height: '48px', borderRadius: '14px',
            background: 'linear-gradient(135deg, #ea580c 0%, #f97316 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center'
          }}>
            <Activity style={{ color: 'white' }} size={24} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Audit Transactions</div>
            <div style={{ fontSize: '1.65rem', fontWeight: 800, color: 'var(--text-main)' }}>
              {stats?.totalTransactions || 0}
            </div>
          </div>
        </div>
      </div>

      {/* Grid: Lines + Traffic Summary */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '2rem', marginBottom: '2rem' }}>
        {/* Mumbai Metro Lines Card */}
        <div className="glass-panel" style={{ padding: '1.75rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <TrendingUp size={20} style={{ color: 'var(--accent-blue)' }} /> Operational Corridors (7 Lines)
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem' }}>
            {lines && Object.entries(lines).map(([code, name]) => (
              <div
                key={code}
                style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                  padding: '0.75rem 1rem', background: 'rgba(255,255,255,0.02)',
                  border: '1px solid rgba(255,255,255,0.08)', borderRadius: '0.75rem'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                  <span className="badge-pill" style={{ background: 'rgba(0, 240, 255, 0.12)', color: '#00f0ff', border: '1px solid rgba(0, 240, 255, 0.3)' }}>
                    {code}
                  </span>
                  <span style={{ fontSize: '0.85rem', fontWeight: 600 }}>{name}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Station Tap Traffic Ranking */}
        <div className="glass-panel" style={{ padding: '1.75rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Activity size={20} style={{ color: 'var(--accent-cyan)' }} /> Station AFC Tap Density (SQLite)
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', maxHeight: '340px', overflowY: 'auto' }}>
            {trafficEntries.slice(0, 10).map(([stName, count], idx) => {
              const pct = (count / maxTraffic) * 100;
              return (
                <div key={idx}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '0.25rem' }}>
                    <span style={{ fontWeight: 600 }}>{stName}</span>
                    <span style={{ color: 'var(--accent-cyan)', fontWeight: 700 }}>{count} taps</span>
                  </div>
                  <div style={{ height: '6px', background: 'rgba(255,255,255,0.08)', borderRadius: '3px', overflow: 'hidden' }}>
                    <div style={{
                      width: `${pct}%`, height: '100%',
                      background: 'linear-gradient(90deg, #00f0ff 0%, #38bdf8 100%)',
                      boxShadow: '0 0 8px rgba(0, 240, 255, 0.5)',
                      borderRadius: '3px', transition: 'width 0.5s ease'
                    }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Transactions Ledger */}
      <div className="glass-panel" style={{ padding: '1.75rem' }}>
        <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Clock size={20} style={{ color: 'var(--accent-emerald)' }} /> Recent Relational Transactions Ledger
        </h3>
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.1)', textAlign: 'left', color: 'var(--text-dim)' }}>
                <th style={{ padding: '0.75rem 1rem' }}>TXN ID</th>
                <th style={{ padding: '0.75rem 1rem' }}>TYPE</th>
                <th style={{ padding: '0.75rem 1rem' }}>REF ID</th>
                <th style={{ padding: '0.75rem 1rem' }}>AMOUNT</th>
                <th style={{ padding: '0.75rem 1rem' }}>ROUTE</th>
                <th style={{ padding: '0.75rem 1rem' }}>STATUS</th>
                <th style={{ padding: '0.75rem 1rem' }}>TIMESTAMP</th>
              </tr>
            </thead>
            <tbody>
              {transactions && transactions.slice(0, 12).map((txn) => (
                <tr key={txn.txnId} style={{ borderBottom: '1px solid rgba(255,255,255,0.04)' }}>
                  <td style={{ padding: '0.75rem 1rem' }}><code style={{ color: '#00f0ff' }}>{txn.txnId}</code></td>
                  <td style={{ padding: '0.75rem 1rem' }}>
                    <span className="badge-pill" style={{ background: 'rgba(0, 240, 255, 0.12)', color: '#00f0ff', border: '1px solid rgba(0, 240, 255, 0.3)' }}>
                      {txn.type}
                    </span>
                  </td>
                  <td style={{ padding: '0.75rem 1rem', fontWeight: 600 }}>{txn.refId || txn.referenceId}</td>
                  <td style={{ padding: '0.75rem 1rem', fontWeight: 700, color: '#ffffff' }}>₹{txn.amount.toFixed(2)}</td>
                  <td style={{ padding: '0.75rem 1rem', color: 'var(--text-muted)' }}>
                    {txn.sourceStation && txn.sourceStation !== '-' ? `${txn.sourceStation} ➔ ${txn.destStation}` : '-'}
                  </td>
                  <td style={{ padding: '0.75rem 1rem' }}>
                    <span className="badge-pill" style={{ background: 'rgba(0, 255, 136, 0.12)', color: '#00ff88', border: '1px solid rgba(0, 255, 136, 0.3)' }}>
                      {txn.status}
                    </span>
                  </td>
                  <td style={{ padding: '0.75rem 1rem', color: 'var(--text-dim)', fontSize: '0.78rem' }}>
                    {txn.createdAt || txn.timestamp}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
