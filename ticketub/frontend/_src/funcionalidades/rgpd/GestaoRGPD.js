import React, { useState, useEffect, useCallback } from 'react';
import {
  getPoliticas, createPolitica, updatePolitica, deletePolitica,
  getExportacoes, decisaoExportacao, direitoAoEsquecimento
} from '../../logica_do_sistema/services/rgpdService';
import './GestaoRGPD.css';

const GestaoRGPD = () => {
  const [activeTab, setActiveTab] = useState('politicas');
  const [politicas, setPoliticas] = useState([]);
  const [exportacoes, setExportacoes] = useState([]);
  const [loading, setLoading] = useState(true);

  // States for policy modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    campo: '',
    metodo: 'HMAC_SHA256',
    retencaoDias: 365,
    aprovadoPor: '',
    notas: ''
  });

  // States for Right to Be Forgotten
  const [forgottenCardId, setForgottenCardId] = useState('');
  const [forgottenMsg, setForgottenMsg] = useState({ text: '', type: '' });
  const [forgottenLoading, setForgottenLoading] = useState(false);

  const fetchPoliticas = useCallback(async () => {
    try {
      const data = await getPoliticas();
      setPoliticas(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Erro a carregar políticas RGPD', err);
      setPoliticas([]);
    }
  }, []);

  const fetchExportacoes = useCallback(async () => {
    try {
      const data = await getExportacoes();
      setExportacoes(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Erro a carregar exportações open data', err);
      setExportacoes([]);
    }
  }, []);

  const loadData = useCallback(async () => {
    setLoading(true);
    if (activeTab === 'politicas') {
      await fetchPoliticas();
    } else if (activeTab === 'exportacoes') {
      await fetchExportacoes();
    }
    setLoading(false);
  }, [activeTab, fetchPoliticas, fetchExportacoes]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Handler for policy modal
  const handleOpenModal = (politica = null) => {
    if (politica) {
      setFormData({
        campo:       politica.campo       || '',
        metodo:      politica.metodo      || 'HMAC_SHA256',
        retencaoDias: politica.retencaoDias || 365,
        aprovadoPor: politica.aprovadoPor  || '',
        notas:       politica.notas        || ''
      });
      setEditingId(politica.id);
    } else {
      setFormData({ campo: '', metodo: 'HMAC_SHA256', retencaoDias: 365, aprovadoPor: '', notas: '' });
      setEditingId(null);
    }
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingId(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingId) {
        await updatePolitica(editingId, formData);
      } else {
        await createPolitica(formData);
      }
      await fetchPoliticas();
      handleCloseModal();
    } catch (err) {
      console.error('Erro a guardar política', err);
    }
  };

  const handleDelete = async (id, campo) => {
    if (window.confirm(`Tem a certeza que deseja revogar a política para o campo "${campo}"?`)) {
      try {
        await deletePolitica(id);
        await fetchPoliticas();
      } catch (err) {
        console.error('Erro a revogar política', err);
      }
    }
  };

  // Handler for Approving/Rejecting Open Data Exports
  const handleDecidirExportacao = async (id, decisao) => {
    const desc = decisao === 'APROVADA' ? 'APROVAR' : 'REJEITAR';
    if (window.confirm(`Tem a certeza que deseja ${desc} este pedido de exportação?`)) {
      setLoading(true);
      try {
        await decisaoExportacao(id, decisao);
        await fetchExportacoes();
      } catch (err) {
        console.error('Erro ao decidir exportacao', err);
        alert('Erro ao registar decisão de exportação.');
      } finally {
        setLoading(false);
      }
    }
  };

  // Handler for Right to Be Forgotten
  const handleEsquecimento = async (e) => {
    e.preventDefault();
    const cleanId = forgottenCardId.trim();
    if (!cleanId) return;

    const confirm1 = window.confirm(`ATENÇÃO: Deseja mesmo invocar o "Direito ao Esquecimento" para o cartão "${cleanId}"?\nEsta ação eliminará todos os eventos históricos e dados operacionais associados.`);
    if (!confirm1) return;

    const confirm2 = window.confirm(`CONFIRMAÇÃO FINAL:\nIsto irá apagar de forma irreversível os dados do cartão do Data Lake (NGSI-LD) e do repositório operacional de picagens.\nConfirmar ação?`);
    if (!confirm2) return;

    setForgottenLoading(true);
    setForgottenMsg({ text: '', type: '' });
    try {
      const res = await direitoAoEsquecimento(cleanId);
      setForgottenMsg({
        text: `Sucesso: ${res.message} (Identificador Hash: ${res.cardIdHashed})`,
        type: 'success'
      });
      setForgottenCardId('');
    } catch (err) {
      setForgottenMsg({
        text: err.message || 'Erro a processar pedido de direito ao esquecimento.',
        type: 'error'
      });
    } finally {
      setForgottenLoading(false);
    }
  };

  return (
    <div className="rgpd-container">
      <div className="rgpd-header">
        <h2>Painel de Gestão e Conformidade RGPD</h2>
      </div>

      <div className="rgpd-tabs">
        <button
          className={`rgpd-tab-btn ${activeTab === 'politicas' ? 'active' : ''}`}
          onClick={() => setActiveTab('politicas')}
        >
          Políticas de Anonimização
        </button>
        <button
          className={`rgpd-tab-btn ${activeTab === 'exportacoes' ? 'active' : ''}`}
          onClick={() => setActiveTab('exportacoes')}
        >
          Aprovação de Exportações
        </button>
        <button
          className={`rgpd-tab-btn ${activeTab === 'esquecimento' ? 'active' : ''}`}
          onClick={() => setActiveTab('esquecimento')}
        >
          Direito ao Esquecimento
        </button>
      </div>

      {activeTab === 'politicas' && (
        <>
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1rem' }}>
            <button className="btn-primary" onClick={() => handleOpenModal()}>
              Nova Política
            </button>
          </div>

          <div className="rgpd-table-container">
            {loading ? (
              <LoadingSpinner msg="A carregar políticas de anonimização..." />
            ) : (
              <table className="rgpd-table">
                <thead>
                  <tr>
                    <th>Campo</th>
                    <th>Método</th>
                    <th>Retenção (Dias)</th>
                    <th>Aprovado por</th>
                    <th>Estado</th>
                    <th>Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {politicas.length > 0 ? (
                    politicas.map((p) => (
                      <tr key={p.id}>
                        <td style={{ fontWeight: 600 }}>{p.campo}</td>
                        <td>{p.metodo}</td>
                        <td>{p.retencaoDias}</td>
                        <td>{p.aprovadoPor}</td>
                        <td>
                          <span className="badge-status approved">{p.estado}</span>
                        </td>
                        <td className="actions-cell">
                          <button className="btn-icon-edit" onClick={() => handleOpenModal(p)}>Editar</button>
                          <button className="btn-icon-delete" onClick={() => handleDelete(p.id, p.campo)}>Revogar</button>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <EmptyState msg="Nenhuma política de anonimização definida." />
                  )}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}

      {activeTab === 'exportacoes' && (
        <div className="rgpd-table-container">
          {loading ? (
            <LoadingSpinner msg="A carregar pedidos de exportação..." />
          ) : (
            <table className="rgpd-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Solicitado Por</th>
                  <th>Formato</th>
                  <th>Período</th>
                  <th>Registos</th>
                  <th>Estado</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {exportacoes.length > 0 ? (
                  exportacoes.map((e) => (
                    <tr key={e.id}>
                      <td>{e.id}</td>
                      <td style={{ fontWeight: 600 }}>{e.requestedBy}</td>
                      <td>{e.format}</td>
                      <td>{e.periodStart} a {e.periodEnd}</td>
                      <td>{e.totalRecords}</td>
                      <td>
                        <span className={`badge-status ${
                          e.status === 'PENDENTE_DPO' ? 'pending' :
                          e.status === 'EXPORTADA' || e.status === 'APROVADA' ? 'approved' : 'rejected'
                        }`}>
                          {e.status}
                        </span>
                      </td>
                      <td className="actions-cell">
                        {e.status === 'PENDENTE_DPO' ? (
                          <>
                            <button
                              className="btn-icon-edit"
                              style={{ backgroundColor: '#ecfdf5', color: '#10b981', borderColor: '#a7f3d0' }}
                              onClick={() => handleDecidirExportacao(e.id, 'APROVADA')}
                            >
                              Autorizar
                            </button>
                            <button
                              className="btn-icon-delete"
                              onClick={() => handleDecidirExportacao(e.id, 'REJEITADA')}
                            >
                              Rejeitar
                            </button>
                          </>
                        ) : (
                          <span style={{ fontSize: '0.8rem', color: '#64748b', fontStyle: 'italic' }}>
                            Decidido por {e.approvedBy || 'DPO'}
                          </span>
                        )}
                      </td>
                    </tr>
                  ))
                ) : (
                  <EmptyState msg="Nenhum pedido de exportação registado no sistema." />
                )}
              </tbody>
            </table>
          )}
        </div>
      )}

      {activeTab === 'esquecimento' && (
        <div className="forgotten-form-card">
          <div className="forgotten-info-box">
            <strong>⚠️ Atenção DPO:</strong> O Direito ao Esquecimento (Artigo 17.º do RGPD) apaga permanentemente todos os registos analíticos e de auditoria operacional do cartão indicado. Esta operação é destrutiva e definitiva.
          </div>

          <form className="modal-form" onSubmit={handleEsquecimento}>
            <div className="form-group">
              <label>Identificador do Cartão (cardId original)</label>
              <input
                type="text"
                value={forgottenCardId}
                onChange={(e) => setForgottenCardId(e.target.value)}
                placeholder="Ex: CARD_12345678"
                required
                disabled={forgottenLoading}
              />
            </div>

            {forgottenMsg.text && (
              <div style={{
                padding: '0.75rem',
                borderRadius: '8px',
                fontSize: '0.85rem',
                backgroundColor: forgottenMsg.type === 'success' ? '#ecfdf5' : '#fef2f2',
                color: forgottenMsg.type === 'success' ? '#065f46' : '#991b1b',
                border: `1px solid ${forgottenMsg.type === 'success' ? '#a7f3d0' : '#fee2e2'}`
              }}>
                {forgottenMsg.text}
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
              <button
                type="submit"
                className="btn-primary"
                style={{ backgroundColor: '#dc2626' }}
                disabled={forgottenLoading || !forgottenCardId.trim()}
              >
                {forgottenLoading ? 'A processar...' : 'Eliminar permanentemente'}
              </button>
            </div>
          </form>
        </div>
      )}

      {isModalOpen && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              {editingId ? 'Editar Política RGPD' : 'Nova Política RGPD'}
            </div>
            <form className="modal-form" onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Campo a Pseudonimizar</label>
                <input
                  type="text"
                  value={formData.campo}
                  onChange={(e) => setFormData({ ...formData, campo: e.target.value })}
                  placeholder="Ex: cardId, ticketId"
                  required
                />
              </div>
              <div className="form-group">
                <label>Método de Anonimização</label>
                <select
                  value={formData.metodo}
                  onChange={(e) => setFormData({ ...formData, metodo: e.target.value })}
                >
                  <option value="HMAC_SHA256">HMAC-SHA256</option>
                  <option value="SUPRESSAO">Supressão</option>
                  <option value="MASCARA">Máscara</option>
                </select>
              </div>
              <div className="form-group">
                <label>Retenção (Dias)</label>
                <input
                  type="number"
                  value={formData.retencaoDias}
                  onChange={(e) => setFormData({ ...formData, retencaoDias: parseInt(e.target.value) })}
                  min="1"
                  required
                />
              </div>
              <div className="form-group">
                <label>Aprovado por (DPO)</label>
                <input
                  type="text"
                  value={formData.aprovadoPor}
                  onChange={(e) => setFormData({ ...formData, aprovadoPor: e.target.value })}
                  placeholder="Nome do DPO"
                  required
                />
              </div>
              <div className="form-group">
                <label>Notas</label>
                <input
                  type="text"
                  value={formData.notas}
                  onChange={(e) => setFormData({ ...formData, notas: e.target.value })}
                  placeholder="Observações opcionais"
                />
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={handleCloseModal}>Cancelar</button>
                <button type="submit" className="btn-primary">Guardar</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

// Subcomponent: Loading Spinner
const LoadingSpinner = ({ msg }) => (
  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '3rem 1rem', gap: '0.5rem', color: '#64748b' }}>
    <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#3b82f6" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="spinning-icon">
      <line x1="12" y1="2" x2="12" y2="6"></line>
      <line x1="12" y1="18" x2="12" y2="22"></line>
      <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line>
      <line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
      <line x1="2" y1="12" x2="6" y2="12"></line>
      <line x1="18" y1="12" x2="22" y2="12"></line>
      <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line>
      <line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
    </svg>
    <span style={{ fontSize: '0.9rem', fontWeight: 600 }}>{msg}</span>
  </div>
);

// Subcomponent: Empty State
const EmptyState = ({ msg }) => (
  <tr>
    <td colSpan="7" style={{ textAlign: 'center', padding: '3rem 1rem' }}>
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.5rem', color: '#64748b' }}>
        <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#64748b" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ opacity: 0.7 }}>
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
        </svg>
        <span style={{ fontSize: '0.9rem', fontWeight: 600 }}>{msg}</span>
      </div>
    </td>
  </tr>
);

export default GestaoRGPD;