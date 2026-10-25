import React, { useState } from 'react';
import { Database, Play, Terminal, Layers, Code, CheckCircle, AlertCircle } from 'lucide-react';

export function AdminConsole({ stats, addToast }) {
  const [sqlQuery, setSqlQuery] = useState("SELECT * FROM stations WHERE line_code = 'LINE_1';");
  const [queryResult, setQueryResult] = useState(null);
  const [isExecuting, setIsExecuting] = useState(false);

  const presets = [
    { label: 'Line 1 Stations', sql: "SELECT * FROM stations WHERE line_code = 'LINE_1';" },
    { label: 'All 90 Stations', sql: 'SELECT * FROM stations;' },
    { label: 'Cards Balance Summary', sql: 'SELECT card_type, COUNT(*) as count, ROUND(SUM(balance),2) as total_balance FROM cards GROUP BY card_type;' },
    { label: 'Recent Transactions', sql: 'SELECT * FROM transactions ORDER BY rowid DESC LIMIT 15;' },
    { label: 'Station Traffic Audit', sql: 'SELECT s.name, s.line_name, COUNT(t.txn_id) as taps FROM stations s LEFT JOIN transactions t ON (s.name = t.source_station OR s.name = t.dest_station) GROUP BY s.id, s.name ORDER BY taps DESC LIMIT 10;' }
  ];

  const handleExecute = async () => {
    if (!sqlQuery.trim()) return;
    setIsExecuting(true);
    try {
      const res = await fetch('/api/admin/query', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sql: sqlQuery })
      });
      const data = await res.json();
      if (data.success) {
        setQueryResult(data);
        addToast(data.message || 'Query executed', 'success');
      } else {
        addToast(data.message || 'SQL Error', 'error');
        setQueryResult({ error: data.message });
      }
    } catch (e) {
      addToast('Query execution failed: ' + e.message, 'error');
    } finally {
      setIsExecuting(false);
    }
  };

  return (
    <div style={{ marginBottom: '2rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
        <div style={{
          width: '42px', height: '42px', borderRadius: '12px',
          background: 'linear-gradient(135deg, #0284c7 0%, #0369a1 100%)',
          display: 'flex', alignItems: 'center', justifyContent: 'center'
        }}>
          <Database style={{ color: 'white' }} size={22} />
        </div>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>
            Embedded SQLite Relational Database Management & SQL Console
          </h2>
          <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
            Direct JDBC execution, schema constraints, transaction audit logs, and query benchmarking
          </p>
        </div>
      </div>

      {/* SQL Editor Panel */}
      <div className="glass-panel" style={{ padding: '1.5rem', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginBottom: '1rem' }}>
          {presets.map((p, idx) => (
            <button
              key={idx}
              onClick={() => setSqlQuery(p.sql)}
              className="btn-transit btn-ghost"
              style={{ fontSize: '0.75rem', padding: '0.35rem 0.75rem' }}
            >
              <Code size={13} /> {p.label}
            </button>
          ))}
        </div>

        <div style={{ position: 'relative' }}>
          <textarea
            value={sqlQuery}
            onChange={e => setSqlQuery(e.target.value)}
            rows={3}
            style={{
              width: '100%',
              padding: '1rem',
              fontFamily: 'var(--font-mono)',
              fontSize: '0.875rem',
              backgroundColor: '#000000',
              color: '#00f0ff',
              border: '1px solid rgba(0, 240, 255, 0.3)',
              borderRadius: '0.75rem',
              outline: 'none',
              resize: 'vertical'
            }}
          />
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.75rem' }}>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-dim)' }}>
            Target Database: <code style={{ color: 'var(--accent-cyan)' }}>{stats?.dbPath || 'mumbai_metro.db'}</code>
          </div>
          <button
            onClick={handleExecute}
            disabled={isExecuting}
            className="btn-transit btn-emerald"
          >
            <Play size={16} /> {isExecuting ? 'Executing...' : 'Run SQL Query'}
          </button>
        </div>
      </div>

      {/* Query Results Viewport */}
      {queryResult && (
        <div className="glass-panel" style={{ padding: '1.5rem', overflow: 'hidden' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3 style={{ fontSize: '1rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Terminal size={18} /> Query Results
            </h3>
            {queryResult.rowCount !== undefined && (
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <span className="badge-pill" style={{ background: 'rgba(0,255,136,0.12)', color: '#00ff88', border: '1px solid rgba(0,255,136,0.3)' }}>
                  {queryResult.rowCount} rows fetched
                </span>
                <span className="badge-pill" style={{ background: 'rgba(0,240,255,0.12)', color: '#00f0ff', border: '1px solid rgba(0,240,255,0.3)' }}>
                  {queryResult.executionTimeMs} ms
                </span>
              </div>
            )}
          </div>

          {queryResult.error ? (
            <div style={{ background: 'rgba(239,68,68,0.15)', border: '1px solid #ef4444', color: '#fca5a5', padding: '1rem', borderRadius: '0.75rem', fontSize: '0.85rem' }}>
              <AlertCircle size={16} style={{ display: 'inline', marginRight: '0.5rem' }} />
              {queryResult.error}
            </div>
          ) : queryResult.rows && queryResult.rows.length > 0 ? (
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.825rem' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid rgba(255,255,255,0.12)', textAlign: 'left', color: 'var(--text-dim)' }}>
                    {queryResult.columnNames.map((col, idx) => (
                      <th key={idx} style={{ padding: '0.65rem 0.85rem' }}>{col}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {queryResult.rows.map((row, rIdx) => (
                    <tr key={rIdx} style={{ borderBottom: '1px solid rgba(255,255,255,0.04)', transition: 'background 0.2s' }}>
                      {row.map((val, cIdx) => (
                        <td key={cIdx} style={{ padding: '0.65rem 0.85rem', color: val === null ? 'var(--text-dim)' : 'var(--text-main)' }}>
                          {val === null ? 'NULL' : String(val)}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div style={{ textAlign: 'center', color: 'var(--text-dim)', padding: '2rem' }}>
              Query executed successfully. 0 rows returned.
            </div>
          )}
        </div>
      )}
    </div>
  );
}
