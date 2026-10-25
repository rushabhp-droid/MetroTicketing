import React, { useState } from 'react';
import { CreditCard, PlusCircle, Lock, Unlock, Shield, GraduationCap, Compass, Wallet, Check } from 'lucide-react';
import confetti from 'canvas-confetti';

export function SmartCards({ cards, onRefreshCards, addToast }) {
  const [showIssueModal, setShowIssueModal] = useState(false);
  const [selectedCardId, setSelectedCardId] = useState(cards[0]?.cardId || '');
  const [rechargeAmount, setRechargeAmount] = useState(150);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // New Card Form State
  const [holderName, setHolderName] = useState('');
  const [contact, setContact] = useState('');
  const [cardType, setCardType] = useState('Regular Card');
  const [initialBalance, setInitialBalance] = useState(100);
  const [college, setCollege] = useState('VJTI Mumbai');
  const [rollNo, setRollNo] = useState('MU-2024-042');

  const handleIssueCard = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      const res = await fetch('/api/cards/issue', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          holderName,
          contact,
          type: cardType,
          initialBalance: parseFloat(initialBalance),
          college,
          rollNo,
          validityDays: 30
        })
      });
      const data = await res.json();
      if (data.success) {
        addToast(data.message, 'success');
        confetti({ particleCount: 50, spread: 60 });
        setShowIssueModal(false);
        setHolderName('');
        setContact('');
        onRefreshCards();
      } else {
        addToast(data.message || 'Issuance failed', 'error');
      }
    } catch (e) {
      addToast('Error issuing card: ' + e.message, 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRecharge = async (cardId, amt) => {
    if (!cardId || amt <= 0) return;
    try {
      const res = await fetch('/api/cards/recharge', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cardId, amount: amt })
      });
      const data = await res.json();
      if (data.success) {
        addToast(data.message, 'success');
        confetti({ particleCount: 40, spread: 50 });
        onRefreshCards();
      } else {
        addToast(data.message || 'Recharge failed', 'error');
      }
    } catch (e) {
      addToast('Network error during recharge', 'error');
    }
  };

  const handleToggleBlock = async (cardId) => {
    try {
      const res = await fetch('/api/cards/toggle-block', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cardId })
      });
      const data = await res.json();
      if (data.success) {
        addToast(data.message, data.isBlocked ? 'error' : 'success');
        onRefreshCards();
      }
    } catch (e) {
      addToast('Error toggling block: ' + e.message, 'error');
    }
  };

  return (
    <div style={{ marginBottom: '2rem' }}>
      {/* Header with Quick Actions */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{
            width: '42px', height: '42px', borderRadius: '12px',
            background: 'linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center'
          }}>
            <CreditCard style={{ color: 'white' }} size={22} />
          </div>
          <div>
            <h2 style={{ fontSize: '1.25rem', fontWeight: 700 }}>
              Polymorphic Smart Cards & AFC Wallet
            </h2>
            <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
              Regular commuter (5% loyalty), Student (20% concession), and Tourist passes
            </p>
          </div>
        </div>

        <button
          onClick={() => setShowIssueModal(!showIssueModal)}
          className="btn-transit btn-primary"
        >
          <PlusCircle size={18} /> Issue New Card
        </button>
      </div>

      {/* Modal / Collapsible Form for Issuing New Card */}
      {showIssueModal && (
        <div className="glass-panel" style={{ padding: '1.75rem', marginBottom: '2rem', border: '1px solid var(--accent-cyan)' }}>
          <h3 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '1rem', color: 'var(--accent-cyan)' }}>
            Issue New Polymorphic Metro Smart Card
          </h3>
          <form onSubmit={handleIssueCard}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.3rem' }}>Passenger Full Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Rahul Sharma"
                  value={holderName}
                  onChange={e => setHolderName(e.target.value)}
                  className="input-transit"
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.3rem' }}>Contact Phone</label>
                <input
                  type="tel"
                  required
                  placeholder="9820011223"
                  value={contact}
                  onChange={e => setContact(e.target.value)}
                  className="input-transit"
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.3rem' }}>Card Category</label>
                <select
                  value={cardType}
                  onChange={e => setCardType(e.target.value)}
                  className="select-transit"
                >
                  <option value="Regular Card">Regular Commuter Card</option>
                  <option value="Student Concession (20% OFF)">Student Pass (20% Concession)</option>
                  <option value="Tourist Pass (3-Day)">Tourist Pass (Unlimited)</option>
                </select>
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.3rem' }}>Initial Balance (₹)</label>
                <input
                  type="number"
                  min="50"
                  step="10"
                  value={initialBalance}
                  onChange={e => setInitialBalance(e.target.value)}
                  className="input-transit"
                />
              </div>
            </div>

            {cardType.includes('Student') && (
              <div style={{
                background: 'rgba(245, 158, 11, 0.08)',
                border: '1px dashed rgba(245, 158, 11, 0.4)',
                borderRadius: '0.75rem', padding: '1rem', marginBottom: '1rem',
                display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem'
              }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', color: '#f59e0b', marginBottom: '0.3rem' }}>College / University</label>
                  <input
                    type="text"
                    value={college}
                    onChange={e => setCollege(e.target.value)}
                    className="input-transit"
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', color: '#f59e0b', marginBottom: '0.3rem' }}>Student ID / Roll No</label>
                  <input
                    type="text"
                    value={rollNo}
                    onChange={e => setRollNo(e.target.value)}
                    className="input-transit"
                  />
                </div>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <button
                type="button"
                onClick={() => setShowIssueModal(false)}
                className="btn-transit btn-ghost"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="btn-transit btn-primary"
              >
                {isSubmitting ? 'Issuing...' : 'Create Card in SQLite'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Holographic Cards Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '1.5rem' }}>
        {cards.map(card => {
          const isStudent = card.collegeName != null || card.studentRollNo != null;
          const isTourist = card.validityDays > 0;
          const isBlocked = card.blocked;

          // Holographic AMOLED pure-black gradient with neon rim
          let bgGradient = 'linear-gradient(135deg, #000000 0%, #041421 50%, #002d4a 100%)';
          let borderGlow = '1px solid rgba(0, 240, 255, 0.35)';
          let badgeText = 'REGULAR CARD';
          let badgeColor = '#00f0ff';
          let icon = <CreditCard size={20} />;

          if (isStudent) {
            bgGradient = 'linear-gradient(135deg, #000000 0%, #201004 50%, #451b03 100%)';
            borderGlow = '1px solid rgba(245, 158, 11, 0.4)';
            badgeText = 'STUDENT PASS (20% OFF)';
            badgeColor = '#f59e0b';
            icon = <GraduationCap size={20} />;
          } else if (isTourist) {
            bgGradient = 'linear-gradient(135deg, #000000 0%, #160726 50%, #3b106b 100%)';
            borderGlow = '1px solid rgba(168, 85, 247, 0.4)';
            badgeText = 'TOURIST 3-DAY PASS';
            badgeColor = '#c084fc';
            icon = <Compass size={20} />;
          }

          if (isBlocked) {
            bgGradient = 'linear-gradient(135deg, #000000 0%, #210505 50%, #4c0707 100%)';
            borderGlow = '1px solid rgba(239, 68, 68, 0.5)';
          }

          return (
            <div key={card.cardId} className="card-holo" style={{ background: bgGradient, border: borderGlow }}>
              {/* Card Header */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.75rem' }}>
                <div>
                  <div style={{ fontSize: '0.7rem', letterSpacing: '0.12em', fontWeight: 800, opacity: 0.85 }}>
                    MUMBAI METRO ONE
                  </div>
                  <span className="badge-pill" style={{ background: 'rgba(0,0,0,0.4)', color: badgeColor, border: `1px solid ${badgeColor}33`, marginTop: '0.35rem' }}>
                    {icon} {badgeText}
                  </span>
                </div>
                <div style={{
                  background: isBlocked ? 'rgba(239,68,68,0.2)' : 'rgba(0,255,136,0.15)',
                  border: `1px solid ${isBlocked ? '#ef4444' : '#00ff88'}`,
                  color: isBlocked ? '#ff4d4d' : '#00ff88',
                  borderRadius: '9999px',
                  padding: '0.2rem 0.6rem',
                  fontSize: '0.7rem',
                  fontWeight: 800
                }}>
                  {isBlocked ? 'BLOCKED' : 'ACTIVE'}
                </div>
              </div>

              {/* Card ID & Balance */}
              <div style={{ marginBottom: '1.5rem' }}>
                <div style={{ fontFamily: 'var(--font-mono)', fontSize: '1.25rem', letterSpacing: '0.1em', fontWeight: 700 }}>
                  {card.cardId}
                </div>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.5rem', marginTop: '0.5rem' }}>
                  <span style={{ fontSize: '0.8rem', opacity: 0.75 }}>BALANCE:</span>
                  <span style={{ fontSize: '1.65rem', fontWeight: 800, color: '#00f0ff' }}>
                    ₹{card.balance.toFixed(2)}
                  </span>
                </div>
              </div>

              {/* Cardholder Info */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', borderTop: '1px solid rgba(255,255,255,0.1)', paddingTop: '0.75rem' }}>
                <div>
                  <div style={{ fontSize: '0.7rem', opacity: 0.65 }}>CARD HOLDER</div>
                  <div style={{ fontWeight: 700, fontSize: '0.9rem' }}>{card.holderName}</div>
                  {isStudent && card.collegeName && (
                    <div style={{ fontSize: '0.7rem', opacity: 0.85 }}>{card.collegeName} ({card.studentRollNo})</div>
                  )}
                </div>

                {/* Quick Actions */}
                <div style={{ display: 'flex', gap: '0.4rem' }}>
                  <button
                    onClick={() => handleRecharge(card.cardId, 100)}
                    className="btn-transit btn-ghost"
                    style={{ padding: '0.35rem 0.6rem', fontSize: '0.75rem', background: 'rgba(255,255,255,0.08)', border: '1px solid rgba(255,255,255,0.18)' }}
                    title="Quick +₹100 Recharge"
                  >
                    +₹100
                  </button>
                  <button
                    onClick={() => handleToggleBlock(card.cardId)}
                    className="btn-transit btn-ghost"
                    style={{ padding: '0.35rem 0.6rem', fontSize: '0.75rem', background: 'rgba(255,255,255,0.08)', border: '1px solid rgba(255,255,255,0.18)' }}
                    title={isBlocked ? 'Unblock card' : 'Block card'}
                  >
                    {isBlocked ? <Unlock size={14} /> : <Lock size={14} />}
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
