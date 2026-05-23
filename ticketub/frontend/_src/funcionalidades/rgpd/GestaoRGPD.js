import React, { useState, useEffect, useCallback } from 'react';
import { getPoliticas, createPolitica, updatePolitica, deletePolitica } from '../../logica_do_sistema/services/rgpdService';
import './GestaoRGPD.css';

const GestaoRGPD = () => {
  const [politicas, setPoliticas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [formData, setFormData] = useState({
    campo: '',
    metodo: 'HMAC_SHA256',
    retencaoDias: 365,
    aprovadoPor: '',
    notas: ''
  });

  const fetchPoliticas = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getPoliticas();
      setPoliticas(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Erro a carregar políticas RGPD', err);
      setPoliticas([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPoliticas();
  }, [fetchPoliticas]);

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

  return (
    <div className="rgpd-container">
      <div className="rgpd-header">
        <h2>Políticas de Anonimização (RGPD)</h2>
        <button className="btn-primary" onClick={() => handleOpenModal()}>
          Nova Política
        </button>
      </div>

      <div className="rgpd-table-container">
        {loading ? (
          <div style={{ padding: '2rem', textAlign: 'center' }}>A carregar políticas...</div>
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
                    <td>{p.estado}</td>
                    <td className="actions-cell">
                      <button className="btn-icon-edit" onClick={() => handleOpenModal(p)}>Editar</button>
                      <button className="btn-icon-delete" onClick={() => handleDelete(p.id, p.campo)}>Revogar</button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="6" style={{ textAlign: 'center', color: '#6b7280', padding: '2rem' }}>
                    Nenhuma política definida.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>

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

export default GestaoRGPD;