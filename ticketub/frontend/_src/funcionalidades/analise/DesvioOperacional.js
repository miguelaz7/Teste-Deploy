import React, { useEffect, useState } from 'react';
import { apiGet } from '../../logica_do_sistema/services/apiClient';
import './DesvioOperacional.css';

const IconCompass = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: '#3b82f6' }}>
    <circle cx="12" cy="12" r="10" />
    <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76" />
  </svg>
);

const IconAlert = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: '#ef4444' }}>
    <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
    <line x1="12" y1="9" x2="12" y2="13" />
    <line x1="12" y1="17" x2="12.01" y2="17" />
  </svg>
);

const IconActivity = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: '#10b981' }}>
    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
  </svg>
);

function DesvioOperacional() {
  const [qualidade, setQualidade] = useState(null);
  const [desvios, setDesvios] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function carregarDados() {
      setLoading(true);
      setError(null);
      try {
        const [resQualidade, resDesvios] = await Promise.all([
          apiGet('/api/desvio-operacional/qualidade-localizacao'),
          apiGet('/api/desvio-operacional/analise-desvios')
        ]);
        setQualidade(resQualidade);
        setDesvios(resDesvios);
      } catch (err) {
        console.error('Erro ao carregar desvios operacionais:', err);
        setError('Não foi possível carregar os dados de desvio operacional.');
      } finally {
        setLoading(false);
      }
    }
    carregarDados();
  }, []);

  if (loading) {
    return (
      <div className="od-container" style={{ textAlign: 'center', padding: '40px' }}>
        <div className="loader-ring">A carregar métricas operacionais...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="od-container" style={{ padding: '24px' }}>
        <div style={{ backgroundColor: '#fee2e2', border: '1px solid #fca5a5', color: '#b91c1c', padding: '16px', borderRadius: '8px' }}>
          ⚠️ {error}
        </div>
      </div>
    );
  }

  return (
    <div className="od-container">
      <div className="map-header" style={{ marginBottom: '24px' }}>
        <h2>Desvio e Correlação Operacional</h2>
        <p>Análise de cruzamento das validações com telemetria GPS (SAE-IP) e desvios de horários da frota TUB.</p>
      </div>

      {/* Alertas de Qualidade UC07.3 */}
      {qualidade && qualidade.alertaDegradacaoQualidade && (
        <div className="degradacao-alerta" style={{
          backgroundColor: '#fffbeb',
          border: '1px solid #fde68a',
          color: '#b45309',
          padding: '12px 16px',
          borderRadius: '8px',
          marginBottom: '16px',
          fontWeight: '500',
          display: 'flex',
          alignItems: 'center',
          gap: '12px'
        }}>
          <IconAlert />
          <div>
            <strong>Alerta de Qualidade de Localização Degradada (UC07.3):</strong> A percentagem de localizações indeterminadas está acima de 10% ({qualidade.percentagemIndeterminada}%). Por favor verifique as ligações dos validadores.
          </div>
        </div>
      )}

      {qualidade && qualidade.alertaSaeIp && (
        <div className="degradacao-alerta" style={{
          backgroundColor: '#fee2e2',
          border: '1px solid #fca5a5',
          color: '#b91c1c',
          padding: '12px 16px',
          borderRadius: '8px',
          marginBottom: '16px',
          fontWeight: '500',
          display: 'flex',
          alignItems: 'center',
          gap: '12px'
        }}>
          <IconAlert />
          <div>
            <strong>Alerta de Falha do Serviço SAE-IP (UC07.3):</strong> A cobertura de localização exata por GPS está abaixo do limiar de 50% face às estimadas por horário.
          </div>
        </div>
      )}

      {/* Grid Superior: Qualidade de Localização vs KPIs Operacionais */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '20px', marginBottom: '24px' }}>
        
        {/* Bloco 1: Qualidade da Localização (UC07.3) */}
        <div className="premium-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
            <IconCompass />
            <strong style={{ fontSize: '1.1rem', color: '#0f172a' }}>Qualidade da Localização (UC07.3)</strong>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '4px' }}>
                <span style={{ color: '#475569', fontWeight: '500' }}>GPS Preciso (Telemetria Real)</span>
                <strong style={{ color: '#10b981' }}>{qualidade?.percentagemGpsPreciso}%</strong>
              </div>
              <div style={{ height: '8px', backgroundColor: '#e2e8f0', borderRadius: '4px', overflow: 'hidden' }}>
                <div style={{ height: '100%', width: `${qualidade?.percentagemGpsPreciso}%`, backgroundColor: '#10b981', borderRadius: '4px' }}></div>
              </div>
            </div>

            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '4px' }}>
                <span style={{ color: '#475569', fontWeight: '500' }}>Estimada por Horário (Schedule)</span>
                <strong style={{ color: '#3b82f6' }}>{qualidade?.percentagemEstimadaSchedule}%</strong>
              </div>
              <div style={{ height: '8px', backgroundColor: '#e2e8f0', borderRadius: '4px', overflow: 'hidden' }}>
                <div style={{ height: '100%', width: `${qualidade?.percentagemEstimadaSchedule}%`, backgroundColor: '#3b82f6', borderRadius: '4px' }}></div>
              </div>
            </div>

            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '4px' }}>
                <span style={{ color: '#475569', fontWeight: '500' }}>Localização Indeterminada</span>
                <strong style={{ color: '#ef4444' }}>{qualidade?.percentagemIndeterminada}%</strong>
              </div>
              <div style={{ height: '8px', backgroundColor: '#e2e8f0', borderRadius: '4px', overflow: 'hidden' }}>
                <div style={{ height: '100%', width: `${qualidade?.percentagemIndeterminada}%`, backgroundColor: '#ef4444', borderRadius: '4px' }}></div>
              </div>
            </div>
          </div>
        </div>

        {/* Bloco 2: Indicadores Planeado vs Executado (UC07.2) */}
        <div className="premium-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
            <IconActivity />
            <strong style={{ fontSize: '1.1rem', color: '#0f172a' }}>Serviço Executado vs. Planeado (UC07.2)</strong>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div style={{ backgroundColor: '#f8fafc', padding: '12px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
              <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', fontWeight: '600' }}>Rácio Procura/Oferta</div>
              <div style={{ fontSize: '1.6rem', fontWeight: '800', color: '#0f172a', marginTop: '4px' }}>{desvios?.procuraVsOfertaRatio}</div>
              <div style={{ fontSize: '0.65rem', color: '#10b981', marginTop: '2px' }}>Dentro da meta TUB</div>
            </div>

            <div style={{ backgroundColor: '#f8fafc', padding: '12px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
              <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', fontWeight: '600' }}>Taxa Geral Atrasos</div>
              <div style={{ fontSize: '1.6rem', fontWeight: '800', color: '#ef4444', marginTop: '4px' }}>{desvios?.taxaAtrasoLinhapct}%</div>
              <div style={{ fontSize: '0.65rem', color: '#ef4444', marginTop: '2px' }}>Atraso &gt; 5 min</div>
            </div>

            <div style={{ gridColumn: 'span 2', backgroundColor: '#f8fafc', padding: '12px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
              <div style={{ fontSize: '0.75rem', color: '#64748b', textTransform: 'uppercase', fontWeight: '600' }}>Zona Crítica Impactada</div>
              <div style={{ fontSize: '1.1rem', fontWeight: '700', color: '#0f172a', marginTop: '4px' }}>{desvios?.impactoGeoAtraso}</div>
            </div>
          </div>
        </div>

      </div>

      {/* Tabela de Desvios de Viagens em Tempo Real (UC07.2) */}
      <div className="premium-card" style={{ padding: '20px' }}>
        <h3 style={{ fontSize: '1.1rem', color: '#0f172a', marginBottom: '16px' }}>Monitorização de Viagens e Desvios em Tempo Real</h3>
        <div style={{ overflowX: 'auto' }}>
          <table className="custom-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #e2e8f0', textAlign: 'left' }}>
                <th style={{ padding: '12px 8px', color: '#475569', fontSize: '0.85rem' }}>Linha / Rota</th>
                <th style={{ padding: '12px 8px', color: '#475569', fontSize: '0.85rem' }}>Atraso Estimado (min)</th>
                <th style={{ padding: '12px 8px', color: '#475569', fontSize: '0.85rem' }}>Velocidade (Real vs Planeada)</th>
                <th style={{ padding: '12px 8px', color: '#475569', fontSize: '0.85rem' }}>Estado Operacional</th>
              </tr>
            </thead>
            <tbody>
              {desvios?.desviosDetalhados.map((item, idx) => (
                <tr key={idx} style={{ borderBottom: '1px solid #f1f5f9' }}>
                  <td style={{ padding: '12px 8px', fontWeight: '600', color: '#0f172a' }}>Linha {item.routeId}</td>
                  <td style={{ padding: '12px 8px', color: '#1e293b' }}>
                    {item.atrasoMinutos === 0 ? 'Sem Atraso' : `+${item.atrasoMinutos} min`}
                  </td>
                  <td style={{ padding: '12px 8px', color: '#475569', fontSize: '0.9rem' }}>
                    {item.velocidadeExecutadaKmh} km/h vs {item.velocidadeEstimadaKmh} km/h
                  </td>
                  <td style={{ padding: '12px 8px' }}>
                    {item.naoApresentacao ? (
                      <span style={{ backgroundColor: '#fee2e2', color: '#b91c1c', padding: '4px 8px', borderRadius: '4px', fontSize: '0.75rem', fontWeight: '600' }}>
                        NÃO-APRESENTAÇÃO
                      </span>
                    ) : item.status === 'ATRASADO_SIGNIFICATIVO' ? (
                      <span style={{ backgroundColor: '#fef2f2', color: '#ef4444', padding: '4px 8px', borderRadius: '4px', fontSize: '0.75rem', fontWeight: '600' }}>
                        ATRASO SIGNIFICATIVO
                      </span>
                    ) : item.status === 'ATRASADO_LIGEIRO' ? (
                      <span style={{ backgroundColor: '#fffbeb', color: '#d97706', padding: '4px 8px', borderRadius: '4px', fontSize: '0.75rem', fontWeight: '600' }}>
                        ATRASO LIGEIRO
                      </span>
                    ) : (
                      <span style={{ backgroundColor: '#d1fae5', color: '#065f46', padding: '4px 8px', borderRadius: '4px', fontSize: '0.75rem', fontWeight: '600' }}>
                        DENTRO DO LIMIAR
                      </span>
                    )}
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

export default DesvioOperacional;
